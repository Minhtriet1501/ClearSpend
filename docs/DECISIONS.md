# Design decisions (ADR)

These records capture the architecture decisions behind ClearSpend, the reasoning for each, and the alternatives that were considered and rejected. The summary version lives in the project [README](../README.md).

## ADR-02. Asynchronous ingestion with SQS, a worker, and a dead letter queue

A scheduler enqueues one page job of the form `{source, page, limit}` per page onto an SQS queue. A worker long polls the queue, fetches that page, normalizes it, and upserts it into MongoDB. A message that fails five times is parked in a dead letter queue.

This design was chosen for isolation, backpressure, and durability. One failing page or source does not block the rest of the batch, because only that message retries. The worker consumes at its own pace, so a slow or bursty source never stalls the read API. At least once delivery combined with the dead letter queue means a bad message is retried and then kept for inspection rather than being dropped, and the scheduler and worker scale and fail independently.

SQS delivers at least once, so a message can be redelivered. Correctness therefore relies on the idempotent upsert described in ADR-03, where reprocessing a job converges to the same state and never duplicates.

The rejected alternative was a synchronous fetch and write loop. In that design one source failing blocks the whole batch, there is no isolation or backpressure, and a retry re runs the entire job.

This was verified in practice. Killing the worker in the middle of a batch let the in flight messages reappear after the visibility timeout, and they were reprocessed so that the document count converged with no loss and no duplicates. One gotcha surfaced during testing: a local worker and the deployed worker polling the same queue compete for messages, so each environment should use its own queue.

## ADR-03. Idempotent upsert on a natural key

The collection has a unique compound index on `(source, externalId)`. Ingest looks up that key, reuses the existing document id when it finds one, and saves. This turns at least once delivery into an effectively exactly once write, which is the reason the asynchronous pipeline in ADR-02 is safe to retry.

## ADR-07. Compute on ECS on EC2, revising the earlier Fargate choice (ADR-05)

The container runs with ECS on an EC2 t3.micro rather than on Fargate. A t3.micro is free tier for 750 hours a month, which means the cost is close to zero running around the clock for the first twelve months, with no need to remember to scale the service down to zero. It also exercises EC2 and ECS on EC2 fundamentals while using the same Docker image and the same ECS control plane.

Fargate was rejected because it bills per task, it has to be scaled to zero to reach no cost, and it has no EC2 free tier. The trade off is that the instance now has to be managed, including operating system patching, and some elasticity is lost, which is acceptable for a single container learning project.

## ADR-08. One CloudFront distribution in front of both the frontend and the backend

A single CloudFront distribution serves the frontend on the default path and routes `/api/*` to the backend on EC2. An HTTPS frontend cannot call an HTTP backend because of mixed content, and putting both behind one CloudFront makes them the same origin. That removes mixed content and CORS, uses the free `*.cloudfront.net` certificate so there is no domain to buy, and needs no load balancer because CloudFront talks straight to the EC2 origin.

Three alternatives were rejected. An application load balancer with an ACM certificate needs an owned domain and costs around sixteen dollars a month always on. Pointing the frontend at a raw EC2 address reintroduces mixed content and depends on an ephemeral IP. Hosting the frontend on Vercel adds another vendor and still leaves the HTTP backend problem unsolved.

## ADR-09. Frontend on a private S3 bucket with CloudFront Origin Access Control

The static build is shipped to a private S3 bucket and served through CloudFront using Origin Access Control. This keeps everything on AWS under one provider and one bill, the bucket is never public because only CloudFront can read it through OAC, and it fits naturally under the shared CloudFront distribution from ADR-08.

Vercel was rejected because it adds a vendor and its HTTPS frontend still cannot reach the HTTP backend. S3 website hosting with a public bucket was rejected because it is less secure and offers no direct HTTPS.

## ADR-10. Explainable statistical anomaly rules computed in the database

Two rules run as MongoDB aggregations and the application applies the thresholds. The spike rule computes a median per agency with `$median` and flags spend above a multiple of that median, marking it high severity at twenty times or more. The concentration rule uses a two stage `$group` and flags a recipient that takes more than half of an agency's spend, marking it high severity at eighty percent or more. Each flag stores a plain language explanation and is idempotent on `(transactionId, rule)`.

Statistical rules were chosen because there are no fraud labels to train a model on, the rules are transparent and auditable, and every flag can be justified to a user. Pushing the heavy aggregation into MongoDB keeps the application thin. A machine learning classifier was rejected because there are no labels and it would not be explainable, and computing medians in the application was rejected because it would pull every row over the wire.

## ADR-11. Database credential in Secrets Manager, injected by ECS

The MongoDB connection string is stored in AWS Secrets Manager. The task definition references it through `secrets.valueFrom`, and ECS injects it as an environment variable when the container starts. This means the connection string, which contains the password, never lives in the task definition or the image. The injection is performed by the ECS execution role, which is granted least privilege access to read only that one secret, and this role is separate from the task role that the application uses for its own SDK calls.

A plaintext environment variable in the task definition was rejected because the credential would then be visible to anyone who can read the task definition.

## ADR-12. CI/CD with GitHub Actions and OIDC, without static AWS keys

GitHub Actions deploys on a push to `main`. It authenticates to AWS through an OIDC identity provider and assumes a scoped IAM role, so there are no AWS access keys stored as repository secrets. Backend images are tagged with the git commit SHA.

Static keys were rejected because they leak and are rarely rotated, whereas OIDC issues short lived credentials scoped to the workflow. Tagging images with the SHA makes the running revision traceable and makes a rollback a single task definition change.

One gotcha was found through CloudTrail. The role kept returning AccessDenied, and the `userIdentity` on the failed call showed that the GitHub OIDC subject carries immutable numeric identifiers in the form `repo:owner@id/repo@id:...`. A trust policy that matched the bare `owner/repo` therefore never matched the real subject, and scoping the trust to the immutable subject fixed it.
