import json, os
import boto3

sqs = boto3.client("sqs")
QUEUE_URL = os.environ["INGEST_QUEUE_URL"]
SOURCES = json.loads(os.environ.get(
    "SOURCES",
    '[{"source":"USAspending","pages":3,"limit":100},{"source":"MesaAZ","pages":3,"limit":100}]'))

def handler(event, context):
    total = 0
    for s in SOURCES:
        for page in range(1, s["pages"] + 1):
            body = json.dumps({"source": s["source"], "page": page, "limit": s["limit"]})
            sqs.send_message(QueueUrl=QUEUE_URL, MessageBody=body)
            total += 1
    print(f"Enqueued {total} page-jobs")
    return {"enqueued": total}