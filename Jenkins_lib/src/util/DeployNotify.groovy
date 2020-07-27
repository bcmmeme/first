#!/usr/bin/groovy
package deploy.util;


def notifyStarted(String doorayNotifyUrl, Map deployInfo) {
	def n = new dooray.Notify()
	def msg = "Deploy started: ${deployInfo.serverGroupName}"
    n.sendMessage(doorayNotifyUrl, "Deploy", msg, [[title: JOB_NAME, titleLink: BUILD_URL, text: "${deployInfo.targetServerHostnames}", color: "blue"]])
}

def notifyFinished(String doorayNotifyUrl, Map deployInfo) {
	
	def buildResult = currentBuild.result
	def notifyColor = "red"		// 실패
	if(currentBuild.result == null) {
	    buildResult = "SUCCESS"
	    notifyColor = "green"		// 성공
	}

	def n = new dooray.Notify()
	def msg = "Deploy ${buildResult}: ${deployInfo.serverGroupName}"	
    n.sendMessage(doorayNotifyUrl, "Deploy", msg, [[title: JOB_NAME, titleLink: BUILD_URL, text: "${deployInfo.targetServerHostnames}", color: notifyColor]])
}
