#!/usr/bin/groovy
package deploy.tcd;
import deploy.util.Helper;



def searchScenario(String scName, Map deployInfo) {

	// like 검색
	String url = "${DEPLOY_API_URL2}/projects/${deployInfo.appkey}/artifacts/${deployInfo.artifactId}/server-group/${deployInfo.serverGroupId}/scenario?scenarioName=${scName}&page=1&pageSize=10"
	def res = httpRequest ignoreSslErrors: true, responseHandle: 'NONE', 
					url: url,
					acceptType: 'APPLICATION_JSON', contentType: 'APPLICATION_JSON',
					consoleLogResponseBody: true, validResponseCodes: '100:200'
	def results = readJSON text: res.content

	if(results.header.isSuccessful == false) {
		error "${url} ${results}"
	}

	// exact search
	for(int i = 0; i < results.body.size(); i++) {
		if(results.body[i].name == scName) {
			return results.body[i].seq
		}
	}

	// not found
	return -1
}


def updateScenario(int scenarioSeq, Map deployInfo, Map scenarioTasks) {

	scenarioTasks["seq"] = scenarioSeq
	String url = "${DEPLOY_API_URL2}/projects/${deployInfo.appkey}/artifacts/${deployInfo.artifactId}/server-group/${deployInfo.serverGroupId}/scenario/${scenarioSeq}"	
	def res = httpRequest ignoreSslErrors: true, responseHandle: 'NONE', 
					url: url,
					httpMode: "POST",
					acceptType: 'APPLICATION_JSON', contentType: 'APPLICATION_JSON',
					consoleLogResponseBody: true, validResponseCodes: '100:200',
					requestBody: (new Helper()).generateBodyAsString(scenarioTasks)
	def results = readJSON text: res.content
	println(results)
}
