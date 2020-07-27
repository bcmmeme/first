#!/usr/bin/groovy
package deploy.tcd;
import deploy.util.Helper;


def select(Map deployInfo) {

	stage("배포 설정") {
		def tcdUtil = new DeployUtil()
		def allServerGroups = tcdUtil.getServerGroups(deployInfo.appkey, deployInfo.artifactId)
		
		def bparams = []
		bparams << string(defaultValue: 'alpha', description: '빌드에 사용됨', name: 'BUILD_PROFILE', trim: false)
		// 핵심: 배포 대상 서버 그룹 선택
		bparams << extendedChoice(defaultValue: '', description: '', descriptionPropertyValue: '', multiSelectDelimiter: ',', 
					name: 'serverGroups', quoteValue: false, saveJSONParameterToFile: false, type: 'PT_CHECKBOX', 
					value: allServerGroups.keySet().asList().join(','), visibleItemCount: 20)
		bparams << extendedChoice(defaultValue: '실행 중단', description: '시나리오 실행 실패 시', multiSelectDelimiter: ',', name: 'nextWhenFail', quoteValue: false, 
				saveJSONParameterToFile: false, type: 'PT_RADIO', value: ['실행 중단','무중단'].join(','), visibleItemCount: 5)
    	bparams << string(name: 'concurrentNum', defaultValue: "1", description: '동시 실행 수, 0이면 동시 배포')
    	bparams << text(name: 'deployNote', defaultValue: "", description: '배포 노트')    	
    	bparams << booleanParam(defaultValue: false, description: 'before install command 실패 무시 여부', name: 'ignore_before_install_failures')
		properties([[$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false], 
			parameters(bparams)
		])

		deployInfo << [ignoreBeforeInstallFailures: params.ignore_before_install_failures,
						concurrentNum: params.concurrentNum]
		if(params.nextWhenFail == "실행 중단") {
			deployInfo << [nextWhenFail: false]
		}else {
			deployInfo << [nextWhenFail: true]		
		}		
		def trigger = (new Helper()).getTrigger()
	    deployInfo << [deployNote: """Executed by ${trigger}
	    ${params.deployNote}
	    """]	    
	    println(deployInfo)
		
		println(params.serverGroups)
		def targetServerGroups = [:]	    
		if(!params.serverGroups) {
    		currentBuild.result = 'ABORTED'
    		error('No server group selected')
		}
		params.serverGroups.split(',').each {
			targetServerGroups.put(it, allServerGroups.get(it))
		}
		deployInfo << [serverGroups: targetServerGroups]
		println(deployInfo)
	}
	manager.addShortText("${params.serverGroups}")

	stage("Confirm") {
		def confirmMsg = """서버 그룹 목록: ${params.serverGroups}
							서버 그룹 하나씩 배포됩니다.
							
							배포 옵션
							- 모든 서버
							- 동시 배포 수: ${deployInfo.concurrentNum} 
							- 실패 시 다음 서버 실행 여부: ${deployInfo.nextWhenFail}
							- before install 동작 실패 무시 여부: ${deployInfo.ignoreBeforeInstallFailures}

							Are you sure?
							"""
		input confirmMsg
	}
}
