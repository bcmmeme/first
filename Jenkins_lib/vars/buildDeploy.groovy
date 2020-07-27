import deploy.*;

// 테스트 항목
// - 정상 배포: 순차 배포 or 동시 배포
//             서버 선택 (일부 서버)
// - 배포 실패: 무중단 or 중단, 
//        순차 배포 or 동시 배포

def call(Map config = [:]) {
 	
 	def availableServerGroups = config.containsKey("availableServerGroups") ? config.availableServerGroups : []
 	def buildFile = config.containsKey("buildSpec") ? config.buildSpec : "build.spec"
 	def deployFile = config.containsKey("deploySpec") ? config.deploySpec : "deploy.spec"
 	def doorayNotifyUrl = config.containsKey("doorayNotifyUrl") ? config.doorayNotifyUrl : ""
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

			def userInput = new tcd.DeployUserInput2()
			userInput.select(availableServerGroups, deployInfo)

			def profile = params.BUILD_PROFILE
			def b = new build.Build()
			b.defaultMavenPackage(profile, buildSpec.buildEnv, buildSpec.packageDirs)
			def u = new build.Upload()
			u.uploadBinary(profile, deployInfo)

			if(deployFile) {
				// 배포 시작 알람
				if(doorayNotifyUrl) {
					def n = new util.DeployNotify()
					n.notifyStarted(doorayNotifyUrl, deployInfo)
				}

				def d = new tcd.Deploy()
				d.execute(deployInfo, deployFile)

				// 배포 완료 알람
				if(doorayNotifyUrl) {
					def n = new util.DeployNotify()
					n.notifyFinished(doorayNotifyUrl, deployInfo)
				}
			}
		}
	}
}
