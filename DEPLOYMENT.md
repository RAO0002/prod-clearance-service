# AWS ECS Fargate Deployment Guide

## Prerequisites

- AWS Account with appropriate IAM permissions
- Docker CLI configured
- AWS CLI v2 installed
- ECS Cluster already created

## Step-by-Step Deployment

### 1. Create ECR Repository

```bash
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
AWS_REGION=us-east-1
REPOSITORY_NAME=prod-clearance-service

aws ecr create-repository \
  --repository-name $REPOSITORY_NAME \
  --region $AWS_REGION \
  --image-scanning-configuration scanOnPush=true \
  --encryption-configuration encryptionType=AES
```

### 2. Build and Push Docker Image

```bash
# Login to ECR
aws ecr get-login-password --region $AWS_REGION | \
  docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

# Build image
docker build -t $REPOSITORY_NAME:latest .

# Tag image
docker tag $REPOSITORY_NAME:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$REPOSITORY_NAME:latest
docker tag $REPOSITORY_NAME:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$REPOSITORY_NAME:$(git rev-parse --short HEAD)

# Push to ECR
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$REPOSITORY_NAME:latest
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$REPOSITORY_NAME:$(git rev-parse --short HEAD)
```

### 3. Create CloudWatch Logs Group

```bash
aws logs create-log-group \
  --log-group-name /ecs/prod-clearance-service \
  --region $AWS_REGION

aws logs put-retention-policy \
  --log-group-name /ecs/prod-clearance-service \
  --retention-in-days 30 \
  --region $AWS_REGION
```

### 4. Store Secrets in AWS Secrets Manager

```bash
# MongoDB URI
aws secretsmanager create-secret \
  --name prod-clearance/mongo-uri \
  --secret-string "mongodb+srv://user:pass@cluster.mongodb.net/prod_clearance" \
  --region $AWS_REGION

# Kafka Bootstrap Servers
aws secretsmanager create-secret \
  --name prod-clearance/kafka-bootstrap-servers \
  --secret-string "kafka-broker-1:9092,kafka-broker-2:9092,kafka-broker-3:9092" \
  --region $AWS_REGION

# Schema Registry URL
aws secretsmanager create-secret \
  --name prod-clearance/schema-registry-url \
  --secret-string "http://schema-registry.example.com:8081" \
  --region $AWS_REGION
```

### 5. Create IAM Roles

#### Task Execution Role (permissions to pull image and write logs)

```bash
cat > ecs-task-execution-role-policy.json << 'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchGetImage",
        "ecr:GetDownloadUrlForLayer"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:log-group:/ecs/prod-clearance-service:*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": "arn:aws:secretsmanager:*:*:secret:prod-clearance/*"
    }
  ]
}
EOF

aws iam create-role \
  --role-name ecsTaskExecutionRole-prod-clearance \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": {"Service": "ecs-tasks.amazonaws.com"},
      "Action": "sts:AssumeRole"
    }]
  }'

aws iam put-role-policy \
  --role-name ecsTaskExecutionRole-prod-clearance \
  --policy-name ecsTaskExecutionPolicy \
  --policy-document file://ecs-task-execution-role-policy.json
```

#### Task Role (permissions for application)

```bash
cat > ecs-task-role-policy.json << 'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "cloudwatch:PutMetricData"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:log-group:/ecs/prod-clearance-service:*"
    }
  ]
}
EOF

aws iam create-role \
  --role-name prod-clearance-service-task-role \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": {"Service": "ecs-tasks.amazonaws.com"},
      "Action": "sts:AssumeRole"
    }]
  }'

aws iam put-role-policy \
  --role-name prod-clearance-service-task-role \
  --policy-name prod-clearance-task-policy \
  --policy-document file://ecs-task-role-policy.json
```

### 6. Register ECS Task Definition

```bash
# Update variables in ecs-task-definition.json
sed -i "s/{ACCOUNT_ID}/$AWS_ACCOUNT_ID/g" ecs-task-definition.json
sed -i "s/{REGION}/$AWS_REGION/g" ecs-task-definition.json

# Register task definition
aws ecs register-task-definition \
  --cli-input-json file://ecs-task-definition.json \
  --region $AWS_REGION
```

### 7. Create ECS Service

```bash
# Get VPC and subnet information
VPC_ID=$(aws ec2 describe-vpcs --filters "Name=tag:Name,Values=prod-vpc" --query 'Vpcs[0].VpcId' --output text)
SUBNET_1=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --query 'Subnets[0].SubnetId' --output text)
SUBNET_2=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --query 'Subnets[1].SubnetId' --output text)
SECURITY_GROUP=$(aws ec2 describe-security-groups --filters "Name=vpc-id,Values=$VPC_ID" "Name=tag:Name,Values=ecs-sg" --query 'SecurityGroups[0].GroupId' --output text)

# Create service
aws ecs create-service \
  --cluster prod-clearance \
  --service-name prod-clearance-service \
  --task-definition prod-clearance-service:1 \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_1,$SUBNET_2],securityGroups=[$SECURITY_GROUP],assignPublicIp=DISABLED}" \
  --load-balancers targetGroupArn=arn:aws:elasticloadbalancing:...,containerName=prod-clearance-service,containerPort=8080 \
  --region $AWS_REGION
```

### 8. Configure Auto Scaling (Optional)

```bash
# Register scalable target
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/prod-clearance/prod-clearance-service \
  --min-capacity 2 \
  --max-capacity 10 \
  --region $AWS_REGION

# Create scaling policy
aws application-autoscaling put-scaling-policy \
  --policy-name prod-clearance-scaling-policy \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/prod-clearance/prod-clearance-service \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration '{
    "TargetValue": 70.0,
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ECSServiceAverageCPUUtilization"
    },
    "ScaleOutCooldown": 60,
    "ScaleInCooldown": 300
  }' \
  --region $AWS_REGION
```

### 9. Verify Deployment

```bash
# Check service status
aws ecs describe-services \
  --cluster prod-clearance \
  --services prod-clearance-service \
  --region $AWS_REGION

# Get running tasks
aws ecs list-tasks \
  --cluster prod-clearance \
  --service-name prod-clearance-service \
  --region $AWS_REGION

# View logs
aws logs tail /ecs/prod-clearance-service --follow --region $AWS_REGION
```

### 10. Set Up CloudWatch Alarms

```bash
# Alarm for high CPU
aws cloudwatch put-metric-alarm \
  --alarm-name prod-clearance-high-cpu \
  --alarm-description "Alert when CPU exceeds 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --alarm-actions arn:aws:sns:$AWS_REGION:$AWS_ACCOUNT_ID:prod-alerts \
  --region $AWS_REGION

# Alarm for high memory
aws cloudwatch put-metric-alarm \
  --alarm-name prod-clearance-high-memory \
  --alarm-description "Alert when memory exceeds 85%" \
  --metric-name MemoryUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --threshold 85 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --alarm-actions arn:aws:sns:$AWS_REGION:$AWS_ACCOUNT_ID:prod-alerts \
  --region $AWS_REGION
```

## Monitoring

### CloudWatch Dashboard

Create a dashboard to visualize:
- Task CPU/Memory utilization
- Error rates and logs
- Kafka publishing metrics
- MongoDB query performance

### Prometheus Integration

Access metrics at: `http://service-endpoint:8080/actuator/metrics/prometheus`

Configure Prometheus scrape job:
```yaml
- job_name: 'prod-clearance-service'
  static_configs:
    - targets: ['prod-clearance.example.com:8080']
  metrics_path: '/actuator/metrics/prometheus'
```

## Rollback Procedure

```bash
# Get previous task definition revision
aws ecs describe-task-definition \
  --task-definition prod-clearance-service:1 \
  --region $AWS_REGION

# Update service to previous revision
aws ecs update-service \
  --cluster prod-clearance \
  --service prod-clearance-service \
  --task-definition prod-clearance-service:1 \
  --region $AWS_REGION
```

## Troubleshooting

### Tasks not starting
```bash
# Check task logs
aws ecs describe-tasks \
  --cluster prod-clearance \
  --tasks <task-arn> \
  --region $AWS_REGION
```

### Service stuck in rolling deployment
```bash
# Force new deployment
aws ecs update-service \
  --cluster prod-clearance \
  --service prod-clearance-service \
  --force-new-deployment \
  --region $AWS_REGION
```
