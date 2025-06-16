def call() {
  pipeline {
    agent any

    environment {
      NODE_ENV = 'production'
      DOCKER_IMAGE = 'rofiqi/portofolio'
      DOCKER_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
      stage('Install Dependencies') {
        steps {
          echo '📦 Installing all dependencies (including devDependencies)...'
          sh 'npm ci --include=dev'
          sh 'npm list --depth=0'
        }
      }

      stage('Lint') {
        steps {
          echo '🔍 Running Lint Check...'
          sh '''
            echo "Current directory: $(pwd)"
            ls -la
            if [ -f package.json ] && grep -q '"lint"' package.json; then
              echo "Found lint script, running..."
              npm run lint
            else
              echo "No lint script found, skipping..."
            fi
          '''
        }
      }

      stage('Build') {
        steps {
          echo '🏗️ Building Application...'
          sh 'npm run build'
          
          echo '📂 Build output:'
          sh 'ls -la dist/'
          sh 'du -sh dist/'
        }
      }

      stage('Build Docker Image') {
        agent {
          docker {
            image 'node:16'
            args '-v /var/run/docker.sock:/var/run/docker.sock'
          }
        }
        steps {
          echo '🐳 Building Docker Image...'
          script {
            sh "docker build -t ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} ."
            sh "docker images ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}"
          }
        }
      }

      stage('Push Image to Docker Hub') {
        agent {
          docker {
            image 'node:16'
            args '-v /var/run/docker.sock:/var/run/docker.sock'
          }
        }
        steps {
          echo '🚀 Starting push image...'
          script {
            try {
              echo '🐳 Using Docker deployment method...'
              withCredentials([usernamePassword(credentialsId: 'docker-hub-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                sh """
                  docker login -u ${DOCKER_USER} -p ${DOCKER_PASS}
                  docker push ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                """
              }

              echo '✅ Deployment successful!'
            } catch (e) {
              echo "❌ Deployment failed: ${e}"
              sh 'printenv'
              throw e
            }
          }
        }
      }

      stage('Deploy to VPS') {
        steps {
          script {
            sshPublisher(
              publishers: [
                sshPublisherDesc(
                  configName: 'vps-1',
                  verbose: true,
                  transfers: [
                    sshTransfer(
                      execCommand: """
                        echo '🐳 Pulling Docker image...'
                        docker pull ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                        
                        echo '🛑 Stopping existing container...'
                        docker stop portfolio-container || true
                        docker rm portfolio-container || true
                        
                        echo '🚀 Starting new container...'
                        docker run -d \
                          --name portfolio-container \
                          -p 80:3000 \
                          ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                        
                        echo '✅ Deployment completed!'
                        docker ps
                      """
                    )
                  ]
                )
              ]
            )
          }
        }
      }
    }

    post {
      always {
        echo '📝 Pipeline completed with status: ${currentBuild.result}'
        archiveArtifacts artifacts: 'dist/**/*', allowEmptyArchive: true
      }
      success {
        echo '✅ Pipeline sukses!'
      }
      failure {
        echo '❌ Pipeline gagal!'
      }
    }
  }
} 