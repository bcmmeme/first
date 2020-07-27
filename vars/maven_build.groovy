def call(){
  node {
    checkout scm
    
    def sayHello() {
    echo "Hello"
    }
  }
}
