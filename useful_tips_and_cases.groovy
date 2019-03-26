//credentials:

node("master"){
    
    def creds

    stage('Sandbox') {
        withCredentials([usernamePassword(credentialsId: 'idHash', passwordVariable: 'C_PASS', usernameVariable: 'C_USER')]) {
            creds = "\nUser: ${C_USER}\nPassword: ${C_PASS}\n"
        }

        println creds
    }
    
}


// confirmation check:

node("master"){
			stage("1"){
			    echo "1"
			}
										stage('Confirmation') {
				//that feeling when confirmation step takes more code than whole pipeline...
				def userInput = false
try {
    def BranchName = "222"
    def DatabaseHost = "32" 	
    def DatabaseName = "123444TS44"
    timeout(time: 15, unit: 'SECONDS') { // change to a convenient timeout for you
						userInput = input(
						id: 'Proceed1',
						message: "\n Update Database ${DatabaseName} \n at Host: ${DatabaseHost}\n from Branch: ${BranchName}\n",
						parameters: [
						[$class: 'BooleanParameterDefinition', defaultValue: false, description: '', name: 'Please check if you agree with this']
						])
    }
				} catch (err) {
					println "Confirmation failure"
					userInput = false
				}

				if (!userInput) {
					currentBuild.result = 'FAILED'
					throw new hudson.AbortException('Pipeline was declined')
				}
				
			}
				
		
			stage("2"){
			    echo "2"
			}
}


//bat handle return status:

node("master")
{
    stage("bat"){
        def returnCode1 = bat returnStatus: true, script:  "rd /S /Q \"C:\\delete_testing\""
        println "return code is - ${returnCode1}"
        if(returnCode1 == 0 || returnCode1 == 2)
        {
            println "path for removing was deleted or even not exists"
        }
        else
        {
            println "there is isses with cleaning of path..."
            currentBuild.result = "FAILED"
			throw new hudson.AbortException("CAN'T REMOVE PATH, SEE DETAILS ABOVE!^")
        }
    }
}

//parse git commands, regex:

node("master"){
    stage("git remote"){
        withCredentials([usernamePassword(credentialsId: 'git_worker_1', passwordVariable: 'PASS', usernameVariable: 'USERNAME')]) {
			try {
				String filterBranch = "AUT"
				String sh_git_result = sh(script: "git ls-remote http://${USERNAME}:${PASS}@bitbucket.com/scm/project.git | grep ${filterBranch}", returnStdout: true).trim()
				println sh_git_result
				
				//regex to take only `release/01.02` from `refs/heads/release/01.02-fake-AUT-123` - (?<=heads\/).*(?<=\d{2}\.\d{2}) 
				// to take only `release/*` -> (?<=heads\/).*
				
				branch_result = (sh_git_result =~ /(?<=heads\/).*/)[0]
				println branch_result
			}
			catch (err){
				println err
			}
        }
    }
}

//jenkins API call example:

stage("call API"){
    node("master"){
        
		String sh_curl_result = sh(script: "curl -u jenkins_user:access_token 'http://localhost:8080/job/Project/job/pipeline/lastSuccessfulBuild/api/xml?tree=actions\\[parameters\\[name,value\\]\\]&xpath=/workflowRun/action/parameter\\[name=%22Branch%22\\]/value'", returnStdout: true).trim()
		println sh_curl_result
        println sh_curl_result[0..-4]
        
        
        result = (sh_curl_result =~ /(?<=>).*(?=<)/)[0]
        println result
        
    }
}


//parallel test:

node('master'){
	stage 'two'
		parallel (
		'parallel_one': {echo "parallel_1"},
		'parallel_two': {echo "parallel_2"},
		'parallel_3': {
				echo "parallel_3"
				parallel (
					'one': {echo "p1"},
					'two': {echo "p1"}
				)
			
			}
		)
}