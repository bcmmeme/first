#!/usr/bin/groovy
package deploy.tcd;

// 서버 그룹 선택
def selectServerGroup(Map deployInfo, String availableEnv = "") {
	def du = new DeployUtil()
	def serverGroups = du.getServerGroups(deployInfo.appkey, deployInfo.artifactId)
	if(serverGroups.size() == 0) {
		error "no server group"
	}
	def allServerGroupNames = serverGroups.keySet().asList()
	def serverGroupNames = []
	if(availableEnv) {
		allServerGroupNames.each {
			if(it.contains(availableEnv)) {
				serverGroupNames << it
			}
		}
	}

	def params = []
	params << choice(name: 'serverGroup', choices: serverGroupNames.join('\n'), description: '서버 그룹 선택')
	params << booleanParam(defaultValue: true, description: '기본 설정 사용 여부 (1대씩 순차 배포)', name: 'useDefault')
	params << booleanParam(defaultValue: false, description: 'ignore-before-install-failures (before install command 실패가 무시됨)', name: 'ignore_before_install_failures')
	def serverGroupInput = input message: '서버 그룹 선택', ok: 'Next', parameters: params	
    return [serverGroupInput.serverGroup, 
    		serverGroups.get(serverGroupInput.serverGroup), 
    		serverGroupInput.useDefault,
    		serverGroupInput.ignore_before_install_failures]
}


// output: deployInfo
def select(Map deployInfo, String availableEnv = "") {
	def du = new DeployUtil()

	def serverGroupName, serverGroupId
	stage("select server group") {
		(serverGroupName, serverGroupId, useDefault, ignore_before_install_failures) = selectServerGroup(deployInfo, availableEnv)
	}
	deployInfo << [serverGroupName: serverGroupName,
					ignore_before_install_failures: ignore_before_install_failures]

	stage("배포 설정") {
		def servers = du.getServers(serverGroupId, deployInfo.appkey, deployInfo.artifactId)
		def listOfServerName = servers.keySet().asList()

		if(useDefault) {
		    deployInfo << [nextWhenFail: false, 
		    		concurrentNum: 1,
		    		targetServerHostnames: listOfServerName,	    		
		    		serverGroupId: serverGroupId]
		    return deployInfo
		}


		def params = []
		// choice에서는 첫번째 항목이 기본 값이다.
		params << choice(name: 'nextWhenFail', choices: ['실행 중단','무중단'].join('\n'), description: '시나리오 실행 실패 시')
	    params << string(name: 'concurrentNum', defaultValue: "1", description: '동시 실행 수, 0이면 동시 배포')
	    listOfServerName.each {
	    	params << booleanParam(name: it, defaultValue: true, description: "배포 서버")
	    }

	    // blue ocean에서 extendedChoice가 지원되지 않는다.
	    //params << extendedChoice(description: 'Select target server', multiSelectDelimiter: ',', name: 'server', quoteValue: false, saveJSONParameterToFile: false, type: 'PT_CHECKBOX', value: '123,456,789', visibleItemCount: 2)
	   
	    params << text(name: 'deployNote', defaultValue: "", description: '배포 노트')

		def deployParams = input message: 'Please deployment parameters', ok: 'Next',
	                parameters: params

	    println(deployParams.nextWhenFail)
	    println(deployParams.concurrentNum)
	    def selectedServers = []
	    listOfServerName.each {
	        println("${it}: ${deployParams[it]}")
	        if(deployParams[it]) {
	        	selectedServers	<< it
	        }
	    }
		
		if(deployParams.deployNote.length() >= 1) {
	    	deployInfo << [deployNote: deployParams.deployNote]
		}    
		if(deployParams.nextWhenFail == "실행 중단") {
			deployInfo << [nextWhenFail: false]
		}else {
			deployInfo << [nextWhenFail: true]
		}

	    deployInfo << [concurrentNum: deployParams.concurrentNum,
	    		targetServerHostnames: selectedServers,	    		
	    		serverGroupId: serverGroupId]
	}
	return deployInfo
}
