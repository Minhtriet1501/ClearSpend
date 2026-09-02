# ClearSpend

Public-finance transparency + anomaly detection. Ingests public spending data from multiple
public sources, normalizes it into MongoDB, flags anomalies with **explainable** rules, and
serves a search dashboard.

**Stack:** Java 21 · Spring Boot 4 · MongoDB Atlas · AWS (ECS Fargate · SQS · EventBridge) · Vercel (frontend)

## Status
🚧 Phase 1 — walking skeleton (in progress).

## Run locally
Requires **Java 21** and **Docker**.

```bash
docker compose up -d       # MongoDB on :27017
./mvnw spring-boot:run      # API on :8080  (JAVA_HOME phải trỏ tới JDK 21)
```

Health check: `curl http://localhost:8080/actuator/health`

## Architecture
_Sơ đồ kiến trúc + ADRs sẽ dán ở đây (cập nhật cuối mỗi phase)._

## Design decisions (ADR — tóm tắt)
- Document store (MongoDB) vs relational — nuốt multi-source schema + giữ raw provenance.
- Async ingestion (SQS + worker + DLQ) vs sync fetch-and-write.
- Idempotent upsert trên `(source, externalId)` — replay không nhân đôi.
- Anomaly = rule thống kê giải thích được (aggregation pipeline), không ML.
- ECS Fargate scale-to-zero khi không demo.
- Raw payload giữ lại (Mongo → S3 sau) để reprocess.
