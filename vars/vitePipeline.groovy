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
          echo 'Linting...'
        //   sh 'npm run lint'
        }
      }

      stage('Build') {
        steps {
          echo 'Building...'
          sh 'npm run build'
        }
      }

      stage('Deploy') {
        // when {
        //   branch 'main'
        // }
        // steps {
        //   deployStaticSite(
        //     remoteUser: 'youruser',
        //     host: 'your.server.com',
        //     path: '/var/www/yourapp'
        //   )
        // }
        steps {
          echo 'Deploying...'
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
