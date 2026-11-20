#!/bin/bash
# Runs on the app server (as root, through SSM Run Command). Loads the secrets from SSM
# Parameter Store, logs in to ECR with the instance role, and (re)starts the stack.
# Usage: IMAGE_TAG=1.0.7 /opt/rentride/deploy.sh
set -euo pipefail
cd "$(dirname "$0")"

: "${IMAGE_TAG:?set IMAGE_TAG, for example IMAGE_TAG=1.0.7}"
PROJECT=${PROJECT:-rentride}

# on a fresh server, wait until user_data has finished installing Docker
cloud-init status --wait > /dev/null || true

TOKEN=$(curl -s -X PUT http://169.254.169.254/latest/api/token -H "X-aws-ec2-metadata-token-ttl-seconds: 60")
REGION=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/placement/region)
ACCOUNT=$(aws sts get-caller-identity --query Account --output text --region "$REGION")
REGISTRY=$ACCOUNT.dkr.ecr.$REGION.amazonaws.com

echo "Loading settings from SSM /$PROJECT/app/"
aws ssm get-parameters-by-path --path "/$PROJECT/app/" --with-decryption --region "$REGION" \
    --query 'Parameters[].[Name,Value]' --output text |
while IFS=$'\t' read -r name value; do
    echo "${name##*/}=$value"
done > app.env
chmod 600 app.env

printf 'REGISTRY=%s\nIMAGE_TAG=%s\n' "$REGISTRY" "$IMAGE_TAG" > .env
mkdir -p uploads

echo "Deploying $IMAGE_TAG from $REGISTRY"
aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$REGISTRY"
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d --remove-orphans
docker image prune -f
docker compose -f docker-compose.prod.yml ps
