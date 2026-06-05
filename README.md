# README - Product Clearance Service

## Overview

Production-ready Spring Boot 4 application for processing group clearance records from MongoDB with Kafka publishing on AWS ECS Fargate. The application continuously polls MongoDB, processes clearance records, and publishes results to Kafka with full resilience and monitoring.

## Features

✅ **Atomic Processing** - Uses MongoDB findAndModify for atomic status updates to prevent duplicate processing  
✅ **Distributed Safety** - Safe for concurrent execution across multiple ECS Fargate tasks  
✅ **Resilience** - Spring Retry with exponential backoff, graceful error handling  
✅ **Monitoring** - Structured logging, Prometheus metrics, health checks  
✅ **Production-Ready** - Full test coverage, Docker containerization, ECS Fargate support  

## Architecture

```
MongoDB (group_process_status)
    ↓
Polling Scheduler (30-second intervals)
    ↓
Atomic Status Update (R → P)
    ↓
Business Logic Processing
    ↓
Kafka Avro Message Generation
    ↓
Schema Registry Integration
    ↓
Kafka Publishing (Avro Serialization)
    ↓
Status Update to Complete (P → C)
```

## Quick Start

### Prerequisites

- Java 25+
- Maven 3.9+
- MongoDB 7.0+
- Kafka with Confluent Schema Registry
- Docker & Docker Compose (for local development)

### Local Development with Docker Compose

```bash
# Start all services
docker-compose up -d

# Build application
mvn clean package

# Run tests
mvn test
mvn verify  # Integration tests

# Access services
# Application: http://localhost:8080
# MongoDB: localhost:27017
# Kafka: localhost:9092
# Schema Registry: http://localhost:8081
# Health: http://localhost:8080/api/health/live
```

### Environment Configuration

```bash
# Core Processing
POLL_INTERVAL_SECONDS=30           # Polling interval
BATCH_SIZE=100                     # Records per batch
MAX_RETRY_ATTEMPTS=3               # Retry attempts
THREAD_POOL_SIZE=5                 # Thread pool size

# MongoDB
MONGO_URI=mongodb://host:27017/prod_clearance

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
SCHEMA_REGISTRY_URL=http://localhost:8081
KAFKA_TOPIC_NAME=clr-delta-prod

# Monitoring
APP_ENV=production
SERVER_PORT=8080
```

## Deployment on AWS ECS Fargate

### 1. Build Docker Image

```bash
mvn clean package
docker build -t prod-clearance-service:latest .

# Push to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin {ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com
docker tag prod-clearance-service:latest {ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/prod-clearance-service:latest
docker push {ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/prod-clearance-service:latest
```

### 2. Configure ECS Task Definition

Update `ecs-task-definition.json`:
- Replace `{ACCOUNT_ID}` and `{REGION}`
- Configure CloudWatch Logs group
- Set IAM roles for task execution and application

### 3. Store Secrets in AWS Secrets Manager

```bash
aws secretsmanager create-secret --name prod-clearance/mongo-uri --secret-string 'mongodb://...'
aws secretsmanager create-secret --name prod-clearance/kafka-bootstrap-servers --secret-string 'kafka.domain.com:9092'
aws secretsmanager create-secret --name prod-clearance/schema-registry-url --secret-string 'http://schema-registry.domain.com:8081'
```

### 4. Deploy to ECS

```bash
# Register task definition
aws ecs register-task-definition --cli-input-json file://ecs-task-definition.json

# Create ECS service
aws ecs create-service \
  --cluster prod-clearance \
  --service-name prod-clearance-service \
  --task-definition prod-clearance-service:1 \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx,subnet-yyy],securityGroups=[sg-xxx]}"
```

## Health Checks & Monitoring

### Endpoints

- **Liveness Probe**: `GET /api/health/live` - Kubernetes/ECS liveness check
- **Readiness Probe**: `GET /api/health/ready` - Kubernetes/ECS readiness check  
- **Statistics**: `GET /api/health/stats` - Processing statistics
- **Metrics**: `GET /actuator/metrics/prometheus` - Prometheus metrics

### Metrics

- `clearance.processing.success` - Successful processing count
- `clearance.processing.failure` - Failed processing count
- `clearance.kafka.published` - Kafka published message count
- `clearance.mongo.queries` - MongoDB query count

### Logging

Structured logging with correlation IDs for request tracing:

```json
{
  "timestamp": "2026-06-05T14:42:00Z",
  "level": "INFO",
  "logger": "com.company.prodclearance.service.ClearanceProcessingService",
  "message": "Processing record rec-123 with productId: G010004451009V",
  "recordId": "rec-123",
  "productId": "G010004451009V",
  "taskId": "TASK-123"
}
```

## Processing Flow

### 1. Polling
- Scheduler runs every 30 seconds
- Queries MongoDB for records with `status='R'` and `type='PROD'`

### 2. Atomic Update
- Uses `findAndModify()` to atomically change status from R → P
- Prevents duplicate processing across multiple tasks
- Increments attemptCount

### 3. Business Processing
- Executes clearance logic for each product
- Generates one or more ClearanceDeltaMessage records
- Handles processing errors gracefully

### 4. Kafka Publishing
- Serializes messages using Confluent Schema Registry
- Publishes to `clr-delta-prod` topic
- Includes correlation headers (record-id, message-id, batch-id)

### 5. Status Update
- On success: Updates status to C (Completed)
- On failure: Updates status to E (Error) with error message
- Updates timestamp

### Error Handling

- **Processing Errors**: Records updated to E (Error) status, processing continues
- **Kafka Errors**: Retried with exponential backoff (max 3 attempts)
- **Database Errors**: Logged and retried, non-blocking

## Testing

### Unit Tests

```bash
mvn test
```

Covers:
- ClearanceProcessingService
- KafkaProducerService
- Repository operations
- Error handling

### Integration Tests

```bash
mvn verify  # or mvn failsafe:integration-test
```

Uses Testcontainers for:
- MongoDB container
- Kafka/Zookeeper containers
- End-to-end processing validation

### Test Coverage

- Processing flow validation
- Concurrent task handling
- Idempotent processing verification
- Error recovery

## Production Considerations

### Scaling

- Horizontal scaling: Deploy multiple ECS tasks
- Atomic MongoDB operations prevent duplicate processing
- Configurable batch size and polling intervals

### Monitoring

- Set up CloudWatch dashboards
- Configure alarms for error rates
- Monitor Kafka lag with consumer group tools

### Performance Tuning

```yaml
app:
  processing:
    poll-interval-seconds: 30        # Adjust based on latency requirements
    batch-size: 100                  # Tune based on record complexity
    thread-pool-size: 5              # Match vCPU count
```

### High Availability

- Multi-AZ deployment with load balancer
- MongoDB replica set with 3 nodes minimum
- Kafka replication factor ≥ 3
- Circuit breaker for external dependencies

## Troubleshooting

### Records Stuck in Processing State

```bash
# Check stuck records
db.group_process_status.find({ status: 'P', updatedDt: { $lt: new Date(Date.now() - 3600000) } })

# Reset stuck records (careful operation)
db.group_process_status.updateMany(
  { status: 'P', updatedDt: { $lt: new Date(Date.now() - 3600000) } },
  { $set: { status: 'R' } }
)
```

### Kafka Publishing Failures

Check Schema Registry connectivity:
```bash
curl http://schema-registry:8081/subjects
```

Verify Kafka topic and replication:
```bash
kafka-topics --describe --topic clr-delta-prod --bootstrap-server localhost:9092
```

### Memory Issues

Increase JVM heap in Dockerfile:
```dockerfile
ENV JAVA_OPTS="-Xmx2048m -Xms1024m ..."
```

## Contributing

- Follow Spring Framework conventions
- Add unit tests for new features
- Run `mvn clean verify` before committing
- Use structured logging with correlation IDs

## License

Proprietary - Company Internal Use Only
