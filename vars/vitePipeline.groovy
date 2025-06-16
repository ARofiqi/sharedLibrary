def call() {
  pipeline {
    agent {
      docker {
        image 'node:16'  # atau image lain yang memiliki docker
        args '-v /var/run/docker.sock:/var/run/docker.sock'
      }
    }


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
        steps {
          echo '🐳 Building Docker Image...'
          script {
            // Verify Dockerfile exists
            // if (!fileExists('Dockerfile')) {
            //   writeFile file: 'Dockerfile', text: """
            //     FROM nginx:alpine
            //     COPY dist/ /usr/share/nginx/html
            //     EXPOSE 80
            //     CMD ["nginx", "-g", "daemon off;"]
            //   """
            //   echo 'ℹ️ Created default Dockerfile for static site'
            // }
            
            sh "docker build -t ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} ."
            sh "docker images | grep ${env.DOCKER_IMAGE}"
          }
        }
      }

      stage('Test Docker Image') {
        steps {
          echo '🔍 Testing Docker Image...'
          script {
            try {
              sh """
                docker run -d --name portofolio -p 8080:80 ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                sleep 5
                curl -s http://localhost:8080 | grep -q '<html' && echo '✅ HTML content found'
                docker stop portofolio
                docker rm portofolio
              """
            } catch (e) {
              error '❌ Docker container test failed'
            }
          }
        }
      }

      stage('Deploy') {
        steps {
          echo '🚀 Starting Deployment...'
          script {
            try {
              // Option 1: Deploy with SSH (original method)
              // echo '🔄 Using SSH deployment method...'
              // sshPublisher(
              //   publishers: [
              //     sshPublisherDesc(
              //       configName: 'vps-1',
              //       verbose: true,
              //       transfers: [
              //         sshTransfer(
              //           sourceFiles: 'dist/**',
              //           removePrefix: 'dist',
              //           remoteDirectory: '/app',
              //           execCommand: 'echo "Deployed files:" && ls -la /app'
              //         )
              //       ]
              //     )
              //   ]
              // )

              // Option 2: Deploy Docker container (alternative method)
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

      stage('Cleanup') {
        steps {
          echo '🧹 Cleaning up...'
          sh "docker rmi ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} || true"
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