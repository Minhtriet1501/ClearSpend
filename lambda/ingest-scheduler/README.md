# Ingest Scheduler (AWS Lambda)

Serverless cron that enqueues ingestion work. **EventBridge** (schedule) → this
**Lambda** → sends one page-job per page to the `clearspend-ingest` **SQS** queue;
the EC2 worker consumes them (see Deployment in the root README).

- Runtime: Python 3.13
- Env: `INGEST_QUEUE_URL` (required), `SOURCES` (optional JSON)

## Deploy an update

```bash
zip function.zip lambda_function.py
aws lambda update-function-code --function-name clearspend-ingest-scheduler \
  --zip-file fileb://function.zip --region us-west-2
```
