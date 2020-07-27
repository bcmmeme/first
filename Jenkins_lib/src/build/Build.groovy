#!/usr/bin/groovy
package deploy.build;

// Sample yaml
// buildEnv:
//   maven: Maven 3.3.9
//   jdk: adoptjdk8
//   command: mvn -B -f ./pom.xml -pl tc-rds-core,tc-rds-api clean package -Dmaven.test.skip=true

def defaultMavenPackage(String mvnProfile, Map buildEnv, List artifactDirs) {
    	
  
	stage("build") {
		def mvn3 = tool name: buildEnv.maven
		def java = tool name: buildEnv.jdk
	    withEnv(["PATH+JDK=${java}/bin", "JAVA_HOME=${java}"]) {
	    	ansiColor('xterm') {
	    		if(buildEnv.command) {
	    			sh script: "${mvn3}/bin/mvn -B ${buildEnv.command}", label: "mvn"
    			}else {
    				if(mvnProfile) {
	   					sh script: "${mvn3}/bin/mvn clean package -Dmaven.test.skip=true -Djacoco.skip=true -P${mvnProfile}", label: "mvn"
					}else {
						sh script: "${mvn3}/bin/mvn clean package -Dmaven.test.skip=true -Djacoco.skip=true", label: "mvn"
					}
	   			}
	   		}
	    }
	}

	stage("package") {
 		sh "rm -rf app.zip"
		zip dir: '', glob: getZipDirs(artifactDirs), zipFile: 'app.zip', archive: true
	}
}


@NonCPS
def getZipDirs(artifactDirs) {
	def t = artifactDirs.collect {
				it + "/**"
			}
	println t
	return t.join(",")

}
