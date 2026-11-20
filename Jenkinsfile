pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }

    parameters {
        booleanParam(name: 'DEPLOY', defaultValue: false,
                description: 'Push the images to ECR and deploy backend and frontend to AWS')
    }

    environment {
        AWS_REGION = 'eu-central-1'
        COMPOSE_PROJECT_NAME = 'rentride'
        IMAGE_TAG = "1.0.${env.BUILD_NUMBER}"
        SERVICES = 'user-service car-service booking-service payment-service notification-service audit-service'
        // from `terraform output` once the infrastructure exists
        FRONTEND_BUCKET = ''
        CLOUDFRONT_ID = ''
    }

    stages {
        stage('Build common') {
            steps {
                dir('common') { sh 'sh ./mvnw -B -q install -DskipTests' }
            }
        }

        stage('Backend: Build and Test') {
            parallel {
                stage('user') { steps { dir('user-service') { sh 'sh ./mvnw -B verify' } } }
                stage('car') { steps { dir('car-service') { sh 'sh ./mvnw -B verify' } } }
                stage('booking') { steps { dir('booking-service') { sh 'sh ./mvnw -B verify' } } }
                stage('payment') { steps { dir('payment-service') { sh 'sh ./mvnw -B verify' } } }
                stage('notification') { steps { dir('notification-service') { sh 'sh ./mvnw -B verify' } } }
                stage('audit') { steps { dir('audit-service') { sh 'sh ./mvnw -B verify' } } }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Backend: Build Images') {
            steps {
                sh 'touch .env'
                sh 'docker compose build'
            }
        }

        stage('Backend: Push to ECR') {
            when { expression { params.DEPLOY } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'aws-jenkins',
                        usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh '''
                        ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
                        REGISTRY=$ACCOUNT.dkr.ecr.$AWS_REGION.amazonaws.com
                        aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $REGISTRY

                        for SERVICE in $SERVICES; do
                            docker tag rentride-$SERVICE $REGISTRY/rentride/$SERVICE:$IMAGE_TAG
                            docker push $REGISTRY/rentride/$SERVICE:$IMAGE_TAG
                        done
                    '''
                }
            }
        }

        stage('Backend: Deploy') {
            when { expression { params.DEPLOY } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'aws-jenkins',
                        usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh '''
                        aws ssm send-command --region $AWS_REGION \
                            --targets Key=tag:Name,Values=rentride-app \
                            --document-name AWS-RunShellScript \
                            --comment "rentride $IMAGE_TAG" \
                            --parameters commands="cd /opt/rentride && IMAGE_TAG=$IMAGE_TAG ./deploy.sh"
                    '''
                }
            }
        }

        stage('Frontend: Build and Deploy') {
            when { expression { params.DEPLOY } }
            steps {
                script {
                    docker.image('node:22-alpine').inside {
                        dir('frontend') { sh 'npm ci && npm run build' }
                    }
                }
                withCredentials([usernamePassword(credentialsId: 'aws-jenkins',
                        usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh '''
                        aws s3 sync frontend/dist s3://$FRONTEND_BUCKET --delete --region $AWS_REGION
                        aws cloudfront create-invalidation --distribution-id $CLOUDFRONT_ID --paths "/*"
                    '''
                }
            }
        }
    }

    post {
        always {
            sh 'docker image prune -f'
        }
    }
}
