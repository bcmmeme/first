#!/usr/bin/groovy
package deploy.salt;


def execute(List<String> servers, List<Map> tasks) {
    
    def logs =[:]       // 서버별 로그 저장
    servers.each {
    	logs[it] = []
    }

    tasks.each {task ->
    	println(task)
    	def r
    	switch(task.type) {
    		case "UserCommand": 
    			r = userCommand(servers, task.command, task.user)
    			break
    		case "BinaryDeploy":
    			r = binaryDeploy(servers, task.binaryUrl, task.path)
    			break
		}
		servers.each {
    		logs[it] << [task: task, log: r[0][it]]
    	}
	}

	printLogs(logs)
}


def printLogs(logs) {

    println("log")

    logs.each { server, taskLogList ->
        //println(logs[it])        
        def output = "<table>"
        taskLogList.each {
            output += "<tr><td>${it.task.type}</td><td><pre>${it.log}</pre></td></tr>"
        }
        output += "</table>"

        manager.createSummary("warning.gif").appendText("<h1>${server}</h1>${output}", false, false, false, "black")
    }
}


def binaryDeploy(List<String> servers, String binaryUrl, String path) {
	//userCommand(servers, "curl -o ${path} ${binaryUrl}")
	return userCommand(servers, "echo binary download", "irteam")
}

def userCommand(List<String> servers, String command, String user) {
	
	def requestBody = [:]
	requestBody["client"] = "local"
	requestBody["expr_form"] = "list"
	requestBody["tgt"] = servers
	requestBody["fun"] = "cmd.run"
	requestBody["arg"] = [command, "runas=${user}"]

// {
//  "client":"local",
//  "expr_form":"list",
//  "tgt":["tcsltcon-01a901", "tcsltcon-01a902", "tcsltapp-01a901"],
//  "fun":"cmd.run",
//  "arg":["echo TEST", "runas=irteam"]
// }

	def headers = []
    
    try {
        def response = httpRequest customHeaders: headers,
                        httpMode: 'POST', requestBody: generateBodyAsString(requestBody), 
                        acceptType: 'APPLICATION_JSON', contentType: 'APPLICATION_JSON',
            			consoleLogResponseBody: true, responseHandle: 'NONE', 
            			validResponseCodes: '100:200',
            			url: 'https://salt.toastoven.net/api/v2/infra?userkey=e08953bfd0c240b4969c75c9d3af337b'
        def result = readJSON text: response.content;
        return result.return
	}catch(e) {
        println(requestBody)
        throw e
    }
}


@NonCPS 
def generateBodyAsString(requestBody) {
    def jsonBuilder = new groovy.json.JsonBuilder()
	jsonBuilder requestBody
	println jsonBuilder.toPrettyString()
	groovy.json.JsonOutput.prettyPrint(jsonBuilder.toString())	
    return jsonBuilder.toString()
}

