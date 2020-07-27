#!/usr/bin/groovy
package deploy.build;

import deploy.util.Helper;
import java.time.*
import java.time.format.*

def uploadBinary(String profile, Map deployInfo) {
    stage("upload") {        
        def artifactId = deployInfo.artifactId
        def appkey = deployInfo.appkey
        def binaryGroupKey = deployInfo.binaryGroupKey
        def t = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyMMdd_HHmmss"))
        def version = "${deployInfo.serverGroupName}_${BUILD_NUMBER}_${BRANCH_NAME}_${profile}_${t}"
        def trigger = (new Helper()).getTrigger()
        def description = """
                        ${BUILD_URL}
                        ${trigger}
                        """

        def url="${DEPLOY_UPLOAD_URL}/${artifactId}";
        sh """curl --fail -o upload_request_result.txt --verbose -i -X POST -H 'Content-Type: multipart/form-data' \
                -F \"appKey=$appkey\" \
                -F \"binaryGroupKey=$binaryGroupKey\" \
                -F \"applicationType=server\" \
                -F \"version=$version\" \
                -F \"description=$description\" \
                -F \"binaryFile=@app.zip\" \
                ${url}"""
        sh 'cat upload_request_result.txt'
        //def result = readFile "upload_request_result.txt"
        //println(result)        
        def tt = sh(returnStdout: true, script: "cat upload_request_result.txt | tail -n 1")
        def res = readJSON text: tt
        println(res)
        if(res.success == false) {
            error "failed to upload ${res}"
        }
        //def binaryKey = sh(returnStdout: true, script: "cat upload_request_result.txt | tail -n 1 |  awk -F ':' '{ print \$2 }' | awk -F ',' '{print \$1}'")
        def binaryKey = res.result
        println("binary key: ${binaryKey}")
        deployInfo["binaryKey"] = binaryKey
    }
}
