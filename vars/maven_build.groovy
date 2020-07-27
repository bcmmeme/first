def call(){
  node {
    checkout scm
    
    def testType
    stage('input'){
      def userInput = input message: 'Choose test type', parameters: [string]
      testType = userInput.testType
    }
    
    stage('build') {
      withEnv["PATH+MAVEN=$tool 'mvn-3.6.0'}/bin"]) {
        sh 'mvn --version'
        sh "mvn clean $(testType)"
      }
    }
    
    stage('report') {
      junit 'target/surefire-reports/*.xml'
      jacoco exePattern: 'target/**.exec'
      addShortText background: '', borderColor: '', color: ''
    }
  }
}
