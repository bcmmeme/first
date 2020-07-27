#!/usr/bin/groovy
package deploy.tcd;

def multiExecute(Map commonDeployInfo, deployFileRule = [:]) {
		
	def deployInfoList = []
	commonDeployInfo.serverGroups.each {
		println("배포: ${it}")
		def du = new DeployUtil()
		def serverGroupId = it.value
		def servers = du.getServers(serverGroupId, commonDeployInfo.appkey, commonDeployInfo.artifactId)
		commonDeployInfo << [	serverGroupId: serverGroupId,
						serverGroupName: it.key,
						targetServerHostnames: servers.keySet().asList()
		]
		
		def deployFile = "deploy.spec"
		if(deployFileRule.containsKey(it.key)) {
			// 예외 룰 적용
			deployFile = deployFileRule.get(it.key)
		}
		// clone()이 shallow copy 이지만 문제될 건 없다.
		deployInfoList << [deployInfo: commonDeployInfo.clone(), deployFile: deployFile]
	}
	println(deployInfoList)


	stage("deploy") {
		// 서버 그룹 하나씩 실행
		deployInfoList.each {
			def deployInfo = it.deployInfo
			def deployFile = it.deployFile
			manager.createSummary("notepad.gif").appendText("<h1>${deployInfo.serverGroupName}</h1>", false, false, false, "black")
			
			def runResult = deployExecute(deployInfo, deployFile)
			waitUntilDeployFinish(runResult.result.resultUrl, runResult.result.deployKey, deployInfo.appkey)
			def s = new DisplayDeployResult()			
			s.htmlResult(deployInfo, runResult.result.deployKey)
		}
	}

}

def execute(Map deployInfo, String deployFile) {

	def runResult	
	stage("deploy started") {
		runResult = deployExecute(deployInfo, deployFile)
	}
	stage("deploy in progress") {
		waitUntilDeployFinish(runResult.result.resultUrl, runResult.result.deployKey, deployInfo.appkey)
	}
	stage("deploy finished") {		
		def s = new DisplayDeployResult()
		//s.printDeployResult(execLogs)
		s.htmlResult(deployInfo, runResult.result.deployKey)
	}
}



// return: scenario id
def deployImport(Map input, Map deployInfo) {

	def jsonFile = 'deploy.json'
	writeJSON file: jsonFile, json: input, pretty: 4    		
    archiveArtifacts jsonFile

    // 시나리오 출력    	
  	//def oo = sh script: "cat ${jsonFile}", returnStdout: true
  	//manager.createSummary("warning.gif").appendText("<pre>${groovy.json.JsonOutput.prettyPrint(oo)}</pre>", false, false, false, "black")  	// <pre> 가 적용되지 않아 pretty 형태로 보이지 않는다. 어디선가 pre tag를 삭제하는 것 같다.
	
	// 파일 업로드 때문에 http request pipeline을 사용할 수 없다.
	// 대신 curl을 사용한다.
	// http request does not support multipart/form-data
	def output = sh script: """curl -X POST ${DEPLOY_API_URL}/importScenario \
   		-H "Content-Type: multipart/form-data" \
   		-F "appKey=${deployInfo.appkey}" \
	   -F "serverGroupId=${deployInfo.serverGroupId}" \
	   -F "artifactId=${deployInfo.artifactId}" \
	   -F "file=@${jsonFile}"
	   """, returnStdout: true
	def importResult = readJSON text: output
	return importResult.result.seq
}



def deployExecute(Map deployInfo, String deployFile) {
	
	def yamlToJson = new YamlToJson()
	def input = yamlToJson.make(deployInfo, deployFile)
	if(deployInfo.binaryGroupName) {
		input["name"] = deployInfo.binaryGroupName + "_build_deploy"
	}else {
		input["name"] = "build_deploy"
	}
	def ds = new DeployScenario()
	def seq = ds.searchScenario(input["name"], deployInfo)
	if(seq == -1) {
		// create new scenario			
		seq = deployImport(input, deployInfo)
	}else {
		// update the existing scenario
		ds.updateScenario(seq, deployInfo, input)
	}		
	int scenarioId = seq
	println("scenario id: ${scenarioId}, scenario name: ${input['name']}")


	def url = "${DEPLOY_API_URL}/runDeploy"
	
	def requestBody = [:]
	requestBody["appKey"] = deployInfo.appkey
	requestBody["artifactId"] = deployInfo.artifactId
	requestBody["serverGroupId"] = deployInfo.serverGroupId
	requestBody["targetServerHostnames"] = deployInfo.targetServerHostnames.join(",")
	requestBody["scenarioId"] = scenarioId
	requestBody["concurrentNum"] = deployInfo.concurrentNum			// 0이면 동시 실행
	requestBody["nextWhenFail"] = deployInfo.nextWhenFail			// true of false	
	
	
	def params = getCurlParameters(requestBody)
	params += " -F deployNote='${deployInfo.deployNote}'"			// '' 로 묶어 주기 위해 별도로 처리
	def output = sh script: """curl --verbose -X POST ${url} \
   		-H "Content-Type: multipart/form-data" \
   		${params}
	   """, returnStdout: true
	println(output)

	
	def runResult
	def t = output.split("<!DOCTYPE html>")
	if(t.length >= 2) {
		try {
			runResult = readJSON text: t[0]
		}catch(e) {
			throw new Exception("might be runDeploy error")
		}
	}else {
		try {
			runResult = readJSON text: output
		}catch(e) {
			throw new Exception("might be runDeploy error")
		}
	}

	println("deploy key: ${runResult.result.deployKey}")
	println("result url: ${runResult.result.resultUrl}")

	return runResult
}


def waitUntilDeployFinish(String resultUrl, int deployKey, String appkey) {
	timeout(10) {
		waitUntil(quiet: true) {
			(finished, execLogs) = isFinished(resultUrl + "async/result/deployLog/${deployKey}?appKey=${appkey}")
			return finished
		}
	}

	if(execLogs.containsKey("result") == false) {
		error("Something is wrong. It might fail to deploy, otherwise please contact to 신현일.")
	}

	// 배포 성공, 실패 확인 
	for(i = 0; i < execLogs.result.size(); i++) {
		def r = execLogs.result[i] 
        if(r.result == "fail") {
        	unstable("배포 실패")
        	return
        }
	}
	println("배포 성공")
}


def getProgress(String resultUrl) {
	// 임시 대응
	// domain으로 접근되지 않아 ip 사용
	def url
	if(resultUrl.contains(DEPLOY_WEB_URL1)) {
		url = resultUrl.replace(DEPLOY_WEB_URL1, DEPLOY_WEB_URL1_IP)
	}else if(resultUrl.contains(DEPLOY_WEB_URL2)) {
		url = resultUrl.replace(DEPLOY_WEB_URL2, DEPLOY_WEB_URL2_IP)
	}else {
		error "result url wrong!!!"
	}
	println(url)

	def output = sh script: "curl --insecure ${url}", returnStdout: true
	output = output.substring(5, output.size() - 1)
	def execLogs = readJSON text: output
	return execLogs	
}

def isFinished(String resultUrl) {
	def execLogs = getProgress(resultUrl)

	// success field가 없으면 execLogs.success 는 null 이다.
	if(execLogs.success == false) {
		println(execLogs)
		// 임시
		// 배포 진행에 변화가 없으면 (실제는 배포 중이라도) timeout이 반환되는 것 같다.
		for(i = 0; i < 3; i++) {
			execLogs = getProgress(resultUrl)
			if(execLogs.success != false) {
				break
			}
		}
		if(execLogs.success == false) {
			println(execLogs)
			return [true, [:]]
		}
	}

	for(i = 0; i < execLogs.result.size(); i++) {
		def r = execLogs.result[i]
        println("execution progress: ${r}") 
        if(!(r.result == "success" || r.result == "fail" || r.result == "cancel")) {
        	// 진행 중
        	return [false, execLogs]
        }
	}
	return [true, execLogs]
}


def getCurlParameters(Map params) {
	def ret = ""
	params.each {
		ret += """-F ${it.key}=${it.value} """
	}
	return ret
}

@NonCPS 
def generateBodyAsString(requestBody) {
    def jsonBuilder = new groovy.json.JsonBuilder()
	jsonBuilder requestBody
	println jsonBuilder.toPrettyString()
	groovy.json.JsonOutput.prettyPrint(jsonBuilder.toString())	
    return jsonBuilder.toString()
}
