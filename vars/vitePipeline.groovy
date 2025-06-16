def call() {
  pipeline {
    agent any

    environment {
      NODE_ENV = 'production'
    }

    stages {
      stage('Install Dependencies') {
        steps {
          echo 'Installing dependencies...'
          sh 'npm ci'
        }
      }

      stage('Lint') {
        steps {
          sh '''
            if [ -f package.json ] && grep -q '"lint"' package.json; then
              npm run lint
            else
              echo "No lint script found, skipping..."
            fi
          '''
        }
      }

      stage('Build') {
        steps {
          echo 'Building...'
          sh 'npm run build'
        }
      }

      stage('Deploy') {
        when {
          branch 'main'
        }
        steps {
          sshPublisher(
            publishers: [
              sshPublisherDesc(
                configName: 'vps-1',
                transfers: [
                  sshTransfer(
                    sourceFiles: 'dist/**',
                    removePrefix: 'dist',
                    remoteDirectory: '/var/www/rofiqi'
                  )
                ]
              )
            ]
          )
        }
      }
    }

    post {
      success {
        echo '✅ Pipeline sukses!'
      }
      failure {
        echo '❌ Pipeline gagal!'
      }
    }
  }
}
