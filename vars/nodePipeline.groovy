def call() {
    pipeline {
        agent any

        stages {
            stage('Test') {
                steps {
                    script {
                        echo "Test"
                    }
                }
            }
            stage('Build') {
                steps {
                    script {
                        echo "build"
                    }
                }
            }
            stage('Deploy') {
                steps {
                    script {
                        echo "deploy"
                    }
                }
            }
        }
    }
}