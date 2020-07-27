import deploy.*;

// deployFileRule:
//  기본 deploy.spec 이 적용된다.
//  deploy.spec이 아닌 파일인 경우 사용한다.
//  [서버그룹명: deploy file, ...]
def call(Map config = [:]) {
	def buildFile = config.containsKey("buildSpec") ? config.buildSpec : "build.spec"
	def deployFileRule = config.containsKey("deploySpecRule") ? config.deploySpecRule : [:]
	def deployEnv = config.containsKey("deployEnv") ? config.deployEnv : "real"

	node("master") {
		checkout scm

		def propAsString = libraryResource "deploy/${deployEnv}_env.prop"
		def tt = readProperties text: propAsString
		println(tt)
		def propList = tt.collect { key, value -> return key+'='+value }

		withEnv(propList) {
			def buildSpec = readYaml file: buildFile
			println buildSpec
			def deployInfo = buildSpec.upload
			println deployInfo

			// 서버그룹 선택
			def userInput = new tcd.DeployUserInput_MultiDeploy()
			userInput.select(deployInfo)

			// 빌드
			def profile = params.BUILD_PROFILE
			def b = new build.Build()
			b.defaultMavenPackage(profile, buildSpec.buildEnv, buildSpec.packageDirs)
			def u = new build.Upload()
			u.uploadBinary(profile, deployInfo)
			
			// 배포			
			def d = new tcd.Deploy()
			d.multiExecute(deployInfo, deployFileRule)
		}
	}


}
