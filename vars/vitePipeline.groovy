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
          script {
            def packageJson = readJSON file: 'package.json'
            if (packageJson.scripts?.lint) {
              sh 'npm run lint'
            } else {
              echo 'No lint script found in package.json, skipping...'
            }
          }
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
