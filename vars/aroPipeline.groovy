def call(Map config) {
    if(config.type == "node"){
        nodePipeline()
    } else {
        pipeline {
        agent any

        stages {
            stage('Unsupported Pipeline') {
                steps {
                    script {
                        echo "Unsupported Pipeline"
                    }
                }
            }
        }
    }
    }
}