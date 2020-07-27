#!/usr/bin/groovy
package deploy.util;

// static 에서는 currentBuild 를 사용할 수 없다.
def getTrigger() {
    try {
        // who trigger this builds
        def causes = currentBuild.getBuildCauses()
        def specificCause = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause')
        def userName = specificCause[0].userName
        def userId = specificCause[0].userId
        return userName
    }catch(e) {
        return "unknown user"            
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
