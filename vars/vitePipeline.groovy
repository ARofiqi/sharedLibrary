def call() {
  pipeline {
    agent any

    environment {
      NODE_ENV = 'production'
    }

    stages {
      stage('Install Dependencies') {
        steps {
          echo '📦 Installing all dependencies (including devDependencies)...'
          sh 'npm ci --include=dev'
          sh 'npm list --depth=0' // Log installed packages
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

      stage('Verify Files') {
        steps {
          sh '''
            echo "📂 Daftar file di dist/:"
            ls -la dist/
            echo "🔍 Cek index.html:"
            if [ -f dist/index.html ]; then
              echo "✅ index.html ada"
              ls -la dist/index.html
            else
              echo "❌ index.html tidak ditemukan!"
              exit 1
            fi
          '''
        }
      }

      stage('Build') {
        steps {
          echo '🏗️ Building...'
          sh 'npm run build'
          
          // Verify build output
          echo '📂 Build output:'
          sh 'ls -la dist/'
          sh 'du -sh dist/' // Show directory size
        }
      }

      stage('Deploy') {
        steps {
          echo '🚀 Starting Deployment...'
          script {
            try {
              // Print SSH configuration details
              echo "🔧 SSH Configuration:"
              echo "- Config Name: vps-1"
              echo "- Remote Directory: /var/www/rofiqi"
              
              sshPublisher(
                publishers: [
                  sshPublisherDesc(
                    configName: 'vps-1',
                    verbose: true, // Enable verbose logging
                    transfers: [
                      sshTransfer(
                        sourceFiles: 'dist/**',
                        removePrefix: 'dist',
                        remoteDirectory: '/var/www/rofiqi',
                        execCommand: 'echo "Deployed files:" && ls -la /var/www/rofiqi' // Verify on remote
                      )
                    ]
                  )
                ]
              )
              echo '✅ Deployment successful!'
            } catch (e) {
              echo "❌ Deployment failed: ${e}"
              sh 'printenv' // Print environment variables for debugging
              throw e
            }
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