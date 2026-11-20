#!/bin/bash
# Builds the service images, pushes them to ECR, deploys them on the app server through
# SSM Run Command, and publishes the frontend to S3 and CloudFront.
#
# Usage (from the repository root, after `terraform apply` in infra/):
#   ./scripts/release.sh                 # tag defaults to 1.0.<timestamp>
#   IMAGE_TAG=1.2.0 ./scripts/release.sh
#
# Needs Docker, the AWS CLI (logged in) and Node.js. FRONTEND_BUCKET and CLOUDFRONT_ID are read
# from the Terraform outputs when not set (CI sets them as environment variables instead).
set -euo pipefail
cd "$(dirname "$0")/.."

export DOCKER_DEFAULT_PLATFORM=linux/amd64
export COMPOSE_PROJECT_NAME=rentride
PROJECT=rentride
SERVICES="user-service car-service booking-service payment-service notification-service audit-service"

IMAGE_TAG=${IMAGE_TAG:-1.0.$(date +%Y%m%d%H%M)}
AWS_REGION=${AWS_REGION:-$(aws configure get region || true)}
AWS_REGION=${AWS_REGION:-eu-north-1}
export AWS_DEFAULT_REGION=$AWS_REGION

tf_output() { terraform -chdir=infra output -raw "$1"; }
FRONTEND_BUCKET=${FRONTEND_BUCKET:-$(tf_output frontend_bucket)}
CLOUDFRONT_ID=${CLOUDFRONT_ID:-$(tf_output cloudfront_id)}

ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
REGISTRY=$ACCOUNT.dkr.ecr.$AWS_REGION.amazonaws.com

echo "==> Building images ($IMAGE_TAG)"
[ -f .env ] || touch .env
docker compose build $SERVICES

echo "==> Pushing images to $REGISTRY"
aws ecr get-login-password | docker login --username AWS --password-stdin "$REGISTRY"
for SERVICE in $SERVICES; do
    docker tag "$PROJECT-$SERVICE" "$REGISTRY/$PROJECT/$SERVICE:$IMAGE_TAG"
    docker push "$REGISTRY/$PROJECT/$SERVICE:$IMAGE_TAG"
done

echo "==> Deploying the backend"
INSTANCE_ID=$(aws ec2 describe-instances \
    --filters "Name=tag:Name,Values=$PROJECT-app" "Name=instance-state-name,Values=running" \
    --query 'Reservations[0].Instances[0].InstanceId' --output text)
[ "$INSTANCE_ID" != "None" ] || { echo "No running $PROJECT-app instance found"; exit 1; }

# a new instance takes a few minutes before SSM can run commands on it
for _ in $(seq 1 60); do
    PING=$(aws ssm describe-instance-information --filters "Key=InstanceIds,Values=$INSTANCE_ID" \
        --query 'InstanceInformationList[0].PingStatus' --output text 2>/dev/null || true)
    [ "$PING" = "Online" ] && break
    echo "    waiting for the server to register with SSM..."
    sleep 10
done

COMPOSE_B64=$(base64 < deploy/docker-compose.prod.yml | tr -d '\n\r')
SCRIPT_B64=$(base64 < deploy/deploy.sh | tr -d '\n\r')
PARAMS=$(mktemp)
cat > "$PARAMS" <<JSON
{"commands": [
  "mkdir -p /opt/$PROJECT",
  "echo $COMPOSE_B64 | base64 -d > /opt/$PROJECT/docker-compose.prod.yml",
  "echo $SCRIPT_B64 | base64 -d > /opt/$PROJECT/deploy.sh",
  "chmod +x /opt/$PROJECT/deploy.sh",
  "IMAGE_TAG=$IMAGE_TAG /opt/$PROJECT/deploy.sh"
]}
JSON

COMMAND_ID=$(aws ssm send-command --instance-ids "$INSTANCE_ID" \
    --document-name AWS-RunShellScript --comment "$PROJECT $IMAGE_TAG" \
    --parameters "file://$PARAMS" --query Command.CommandId --output text)
rm -f "$PARAMS"

STATUS=Pending
for _ in $(seq 1 120); do
    sleep 5
    STATUS=$(aws ssm get-command-invocation --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" \
        --query Status --output text 2>/dev/null || echo Pending)
    case "$STATUS" in Pending|InProgress|Delayed) continue ;; *) break ;; esac
done
aws ssm get-command-invocation --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" \
    --query StandardOutputContent --output text | tail -20
[ "$STATUS" = "Success" ] || { echo "Backend deploy finished with status $STATUS"; exit 1; }

echo "==> Publishing the frontend"
(cd frontend && npm ci && npm run build)
aws s3 sync frontend/dist "s3://$FRONTEND_BUCKET" --delete
aws cloudfront create-invalidation --distribution-id "$CLOUDFRONT_ID" --paths "/*" > /dev/null

echo "==> Released $IMAGE_TAG"
