pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }

    triggers {
        pollSCM('H/2 * * * *')
    }

    parameters {
        booleanParam(name: 'DEPLOY', defaultValue: false,
                description: 'Push the images to ECR and deploy backend and frontend to AWS')
    }

    environment {
        AWS_REGION = 'eu-central-1'
        COMPOSE_PROJECT_NAME = 'rentride'
        IMAGE_TAG = "1.0.${env.BUILD_NUMBER}"
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

        stage('Deploy to AWS') {
            when { expression { params.DEPLOY } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'aws-jenkins',
                        usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh 'sh ./scripts/release.sh'
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
