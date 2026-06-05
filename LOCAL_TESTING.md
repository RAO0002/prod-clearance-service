# Local Testing Guide for Product Clearance Service

## Prerequisites

### Required Software
- **Java 25** - [Download](https://jdk.java.net/25/)
- **Maven 3.9+** - [Download](https://maven.apache.org/download.cgi)
- **Docker Desktop** - [Download](https://www.docker.com/products/docker-desktop)
- **Docker Compose** (included with Docker Desktop)
- **Git** - [Download](https://git-scm.com/)

### Verify Installation

```bash
java -version
# Output: openjdk version "25" 2024-09-17

mvn -version
# Output: Apache Maven 3.9.x

docker --version
# Output: Docker version 27.x.x

docker-compose --version
# Output: Docker Compose version 2.x.x
```

---

## Option 1: Quick Start with Docker Compose (Recommended)

### Step 1: Clone Repository

```bash
git clone https://github.com/RAO0002/prod-clearance-service.git
cd prod-clearance-service
```

### Step 2: Start All Services

```bash
# Start MongoDB, Kafka, Schema Registry, and application
docker-compose up -d

# Check status
docker-compose ps
```

**Expected Output:**
```
NAME                        STATUS              PORTS
prod-clearance-zookeeper    Up 2 minutes        2181/tcp
prod-clearance-kafka        Up 2 minutes        9092/tcp
prod-clearance-mongo        Up 2 minutes        27017/tcp
prod-clearance-schema-registry Up 2 minutes     8081/tcp
prod-clearance-service      Up 1 minute         8080/tcp
```

### Step 3: Verify Services

```bash
# Check application is running
curl http://localhost:8080/api/health/live
# Response: UP

# Check readiness
curl http://localhost:8080/api/health/ready
# Response: READY

# Check statistics
curl http://localhost:8080/api/health/stats
# Response: JSON with metrics

# Check MongoDB
docker-compose exec mongo mongosh
> db.group_process_status.countDocuments()
> exit

# Check Kafka
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Check Schema Registry
curl http://localhost:8081/subjects
```

### Step 4: Insert Test Data

```bash
# Connect to MongoDB
docker-compose exec mongo mongosh

# Use the database
> use prod_clearance

# Insert test records
> db.group_process_status.insertMany([
  {
    _id: "test-rec-1",
    trackNo: null,
    type: "PROD",
    productId: "G010004451009V",
    trackExt: null,
    status: "R",
    updatedDt: new Date(),
    awsRequestId: "aws-req-001",
    attemptCount: 0
  },
  {
    _id: "test-rec-2",
    trackNo: null,
    type: "PROD",
    productId: "G010004451010V",
    trackExt: null,
    status: "R",
    updatedDt: new Date(),
    awsRequestId: "aws-req-002",
    attemptCount: 0
  },
  {
    _id: "test-rec-3",
    trackNo: null,
    type: "PROD",
    productId: "G010004451011V",
    trackExt: null,
    status: "R",
    updatedDt: new Date(),
    awsRequestId: "aws-req-003",
    attemptCount: 0
  }
])

# Verify insertion
> db.group_process_status.find().pretty()

# Exit
> exit
```

### Step 5: Monitor Processing

```bash
# Watch application logs
docker-compose logs -f app

# In another terminal, check Kafka messages
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server kafka:9092 \
  --topic clr-delta-prod \
  --from-beginning

# In another terminal, check MongoDB status updates
watch -n 5 'docker-compose exec -T mongo mongosh --quiet --eval "db.group_process_status.find({}, {status: 1, productId: 1, _id: 0}).pretty()"'
```

### Step 6: View Processed Records

```bash
# Check record status after processing
docker-compose exec mongo mongosh

> use prod_clearance
> db.group_process_status.find().pretty()

# Should show status changed from "R" to "C" (Completed)
```

### Step 7: Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

---

## Option 2: Manual Setup (Advanced)

### Step 1: Install Services Individually

#### MongoDB
```bash
# macOS (Homebrew)
brew tap mongodb/brew
brew install mongodb-community
brew services start mongodb-community

# Linux (Ubuntu)
curl https://www.mongodb.org/static/pgp/server-7.0.asc | sudo apt-key add -
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu focal/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list
sudo apt-get update
sudo apt-get install -y mongodb-org
sudo systemctl start mongod

# Verify
mongosh
> use admin
> db.adminCommand("ping")
> exit
```

#### Kafka & Zookeeper
```bash
# Download Confluent Platform
cd /tmp
wget https://packages.confluent.io/archive/7.7/confluent-7.7.0.tar.gz
tar xzf confluent-7.7.0.tar.gz
cd confluent-7.7.0

# Start Zookeeper
./bin/zookeeper-server-start -daemon ./etc/kafka/zookeeper.properties

# Start Kafka
./bin/kafka-server-start -daemon ./etc/kafka/server.properties

# Start Schema Registry
./bin/schema-registry-start -daemon ./etc/schema-registry/schema-registry.properties

# Verify
curl http://localhost:8081/subjects
```

### Step 2: Build Application

```bash
cd /path/to/prod-clearance-service
mvn clean install
```

### Step 3: Run Application

```bash
# Set environment variables
export MONGO_URI=mongodb://localhost:27017/prod_clearance
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export SCHEMA_REGISTRY_URL=http://localhost:8081
export POLL_INTERVAL_SECONDS=10
export BATCH_SIZE=50

# Run application
mvn spring-boot:run
```

---

## Running Tests Locally

### Unit Tests

```bash
# Run all unit tests
mvn test

# Run specific test class
mvn test -Dtest=ClearanceProcessingServiceTest

# Run with coverage
mvn test jacoco:report
# View coverage at: target/site/jacoco/index.html
```

### Integration Tests

```bash
# Run integration tests (requires Docker and Testcontainers)
mvn verify

# Run specific integration test
mvn verify -Dit.test=GroupProcessStatusRepositoryIT

# Skip unit tests, run only integration tests
mvn verify -DskipTests
```

### Test Output Example

```bash
$ mvn test

[INFO] --- maven-surefire-plugin:3.2.5:test (default-test) @ prod-clearance-service ---
[INFO] Running com.company.prodclearance.service.ClearanceProcessingServiceTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.345 s

[INFO] Running com.company.prodclearance.kafka.KafkaProducerServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.234 s

[INFO] BUILD SUCCESS
```

---

## Manual Testing with curl

### Test Health Endpoints

```bash
# Liveness probe
curl -v http://localhost:8080/api/health/live

# Readiness probe
curl -v http://localhost:8080/api/health/ready

# Statistics
curl http://localhost:8080/api/health/stats | jq
```

### Monitor Metrics

```bash
# Prometheus metrics
curl http://localhost:8080/actuator/metrics/prometheus | grep clearance

# Specific metrics
curl http://localhost:8080/actuator/metrics/clearance.processing.success
curl http://localhost:8080/actuator/metrics/clearance.processing.failure
curl http://localhost:8080/actuator/metrics/clearance.kafka.published
```

### Check Application Logs

```bash
# Docker Compose
docker-compose logs -f app --tail=100

# Local run
# Logs are written to: logs/prod-clearance-service.log
tail -f logs/prod-clearance-service.log
```

---

## End-to-End Testing Workflow

### Scenario: Process a Single Record

```bash
# 1. Insert a test record
docker-compose exec mongo mongosh << 'EOF'
use prod_clearance
db.group_process_status.insertOne({
  _id: "e2e-test-001",
  type: "PROD",
  productId: "E2E-TEST-001",
  status: "R",
  updatedDt: new Date(),
  awsRequestId: "e2e-aws-001",
  attemptCount: 0
})
db.group_process_status.findOne({_id: "e2e-test-001"})
EOF

# 2. Watch application process it (in separate terminal)
docker-compose logs -f app | grep "e2e-test-001"

# 3. Wait 30 seconds for polling cycle

# 4. Verify status changed to "C" (Completed)
docker-compose exec mongo mongosh << 'EOF'
use prod_clearance
db.group_process_status.findOne({_id: "e2e-test-001"})
EOF

# 5. Check Kafka message was published
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server kafka:9092 \
  --topic clr-delta-prod \
  --from-beginning \
  --max-messages 1
```

### Scenario: Test Error Handling

```bash
# 1. Insert a record that will fail processing
docker-compose exec mongo mongosh << 'EOF'
use prod_clearance
db.group_process_status.insertOne({
  _id: "error-test-001",
  type: "PROD",
  productId: "ERROR-PRODUCT",
  status: "R",
  updatedDt: new Date(),
  awsRequestId: "error-aws-001",
  attemptCount: 0
})
EOF

# 2. Trigger processing manually (optional)
# Kill and restart app to trigger new polling cycle

# 3. Check for error status
docker-compose exec mongo mongosh << 'EOF'
use prod_clearance
db.group_process_status.findOne({_id: "error-test-001"})
# Should show: status: "E", errorMessage: "..."
EOF

# 4. Check application logs for error message
docker-compose logs app | grep "error-test-001"
```

### Scenario: Load Test with Batch Processing

```bash
# 1. Insert 500 test records
docker-compose exec mongo mongosh << 'EOF'
use prod_clearance
const records = [];
for (let i = 1; i <= 500; i++) {
  records.push({
    _id: `load-test-${String(i).padStart(4, '0')}`,
    type: "PROD",
    productId: `LOAD-PROD-${String(i).padStart(4, '0')}`,
    status: "R",
    updatedDt: new Date(),
    awsRequestId: `load-aws-${i}`,
    attemptCount: 0
  });
}
db.group_process_status.insertMany(records);
db.group_process_status.countDocuments({status: "R"});
EOF

# 2. Monitor processing
docker-compose logs -f app | grep -E "Processing|completed|Batch"

# 3. Watch batch complete
watch -n 5 'docker-compose exec -T mongo mongosh --quiet --eval "db.group_process_status.aggregate([{$group: {_id: \"$status\", count: {$sum: 1}}}]).pretty()"'

# 4. Generate report after completion
docker-compose exec mongo mongosh << 'EOF'
use prod_clearance
console.log("Processing Summary:");
db.group_process_status.aggregate([
  {$group: {_id: "$status", count: {$sum: 1}}},
  {$sort: {_id: 1}}
]).forEach(doc => console.log(doc._id + ": " + doc.count));
EOF
```

---

## Database Inspection

### MongoDB Commands

```bash
# Connect to MongoDB
docker-compose exec mongo mongosh

# View all records with status
> use prod_clearance
> db.group_process_status.find({}, {_id: 1, productId: 1, status: 1, updatedDt: 1})

# Count by status
> db.group_process_status.aggregate([
  {$group: {_id: "$status", count: {$sum: 1}}}
])

# Find errors
> db.group_process_status.find({status: "E"})

# View processing times
> db.group_process_status.find({status: "C"}, {productId: 1, updatedDt: 1}).limit(5)

# Clean up test data
> db.group_process_status.deleteMany({_id: /test/})
> db.group_process_status.deleteMany({})
```

---

## Kafka Inspection

### Kafka Commands

```bash
# List topics
docker-compose exec kafka kafka-topics \
  --list \
  --bootstrap-server kafka:9092

# Describe topic
docker-compose exec kafka kafka-topics \
  --describe \
  --topic clr-delta-prod \
  --bootstrap-server kafka:9092

# View messages
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server kafka:9092 \
  --topic clr-delta-prod \
  --from-beginning \
  --max-messages 10

# View consumer group lag
docker-compose exec kafka kafka-consumer-groups \
  --bootstrap-server kafka:9092 \
  --describe \
  --group console-consumer-group
```

---

## Troubleshooting Local Setup

### Issue: Port Already in Use

```bash
# Find process using port
lsof -i :8080    # Application
lsof -i :27017   # MongoDB
lsof -i :9092    # Kafka
lsof -i :8081    # Schema Registry

# Kill process (if needed)
kill -9 <PID>

# Or use different ports in docker-compose.yml
```

### Issue: Docker Compose Services Won't Start

```bash
# Check logs
docker-compose logs mongo
docker-compose logs kafka
docker-compose logs app

# Rebuild images
docker-compose build --no-cache

# Start with verbose output
docker-compose up --verbose
```

### Issue: Application Not Connecting to MongoDB

```bash
# Test MongoDB connectivity
docker-compose exec app nc -zv mongo 27017

# Check MongoDB logs
docker-compose logs mongo

# Verify connection string
echo $MONGO_URI
```

### Issue: Kafka Messages Not Publishing

```bash
# Check Kafka logs
docker-compose logs kafka

# Verify Schema Registry is running
curl http://localhost:8081/config

# Test Kafka connection
docker-compose exec app kafka-broker-api-versions \
  --bootstrap-server kafka:9092
```

### Issue: OutOfMemory or Slow Performance

```bash
# Increase Docker memory limit
# Docker Desktop Settings → Resources → Memory: 8GB+

# Adjust Kafka broker settings in docker-compose.yml
environment:
  KAFKA_HEAP_OPTS: "-Xmx1G -Xms512M"

# Adjust MongoDB settings
environment:
  MONGOD_OPTS: "--wiredTigerCacheSizeGB=2"
```

---

## Performance Testing

### Test Processing Speed

```bash
# Insert 100 records and measure processing time
docker-compose exec mongo mongosh << 'EOF'
use prod_clearance
const startTime = Date.now();
const records = [];
for (let i = 1; i <= 100; i++) {
  records.push({
    _id: `perf-test-${i}`,
    type: "PROD",
    productId: `PERF-${i}`,
    status: "R",
    updatedDt: new Date(),
    awsRequestId: `perf-aws-${i}`
  });
}
db.group_process_status.insertMany(records);
print("Inserted in: " + (Date.now() - startTime) + "ms");
EOF

# Monitor until all processed
watch -n 2 'docker-compose exec -T mongo mongosh --quiet --eval "db.group_process_status.countDocuments({status: \"C\"})"'

# Calculate statistics
docker-compose logs app | grep "successfully processed" | wc -l
```

---

## Cleanup

```bash
# Stop all services
docker-compose down

# Remove volumes (reset database)
docker-compose down -v

# Remove images
docker-compose down --rmi all

# Remove application logs
rm -rf logs/
```

---

## Next Steps

Once comfortable with local testing:

1. **Modify Business Logic** - Edit `ClearanceProcessingService.executeClearanceLogic()`
2. **Add More Test Cases** - Extend integration tests in `src/test/`
3. **Deploy to Docker Hub** - Push image for sharing
4. **Deploy to AWS ECS** - Follow `DEPLOYMENT.md`

