#!groovy

@Library('projctsharedlibrary')_
import com.companyname.Inventory
import com.companyname.Server
import com.companyname.DB
import com.companyname.Client
import com.companyname.Builder


def BuildWorkerId = 'Node-hostname' // Node-hostname == Node-hostname
def repoURL = 'ssh://git@bitbucket.companyname.com:7999/Project.git'
def env_config = 'test' // test, dev, uat
def newDBName = 'Project_Test_pipeline'

userEmail = getProperty('UserEmail').toString()
BRANCH_NAME = getProperty('BranchName').toString()
CommitID = getProperty('CommitIDHash').toString()
ForceBuild = getProperty('ForceBuild').toBoolean()
RestoreFromBackupAndUpdate = getProperty('RestoreFromBackupAndUpdate').toBoolean()
alreadybuilt = 0 // redis cache flag

builder = new Builder()
DB db = null
Server server = null


currentBuild.displayName = currentBuild.number + " - " + BRANCH_NAME


node("master"){
	println currentBuild.result
	notifyBitbucket commitSha1: CommitID, considerUnstableAsSuccess: false, credentialsId: 'jenkins_credentials_id', disableInprogressNotification: false, ignoreUnverifiedSSLPeer: true, includeBuildNumberInKey: false, prependParentProjectKey: false, projectKey: '', stashServerBaseUrl: 'http://bitbucket.companyname.com'
	
	stage("Build state check"){

		try {
			String test_redis = sh(script: "/usr/local/bin/redis-cli set testkey testvalue", returnStdout: true).trim()
			//println test_redis
			}
		catch (err) 
		{
			println err
			println "Looks like Redis is not available, use - sudo systemctl start redis_6379 - on jenkins master node [hostname]; or  - /usr/local/bin/redis-server - in screen/tmux'"
			mail body: 'Redis is not available, use - sudo systemctl start redis_6379 - on jenkins master node [hostname]; or  - /usr/local/bin/redis-server - in screen/tmux',
				from: 'JenkinsRedis@companyname.com', subject: 'Redis is not available', to: 'Artur@companyname.com', cc: 'Alexander@companyname.com'
		}
			
		try {
			String sh_result = sh(script: "/usr/local/bin/redis-cli get ${JOB_NAME}:${CommitID}", returnStdout: true).trim()
			println sh_result
			
			if(!sh_result){
				println "EMPTY RESULT"
				println "no such value for ${JOB_NAME}:${CommitID}"
				alreadybuilt = 0 // just in case
				}
			
			if(sh_result == "SUCCESS"){
				println "Previous build result for ${CommitID} is: ${sh_result}"
				println ForceBuild
				if (ForceBuild == true){
					alreadybuilt = 0
					println "Force build trigger found, commit will be re-built"
				}
				else
				{
					alreadybuilt = 1
					println currentBuild.result
					currentBuild.result = "SUCCESS"
					println currentBuild.result
					notifyBitbucket commitSha1: CommitID, considerUnstableAsSuccess: false, credentialsId: 'jenkins_credentials_id', disableInprogressNotification: false, ignoreUnverifiedSSLPeer: true, includeBuildNumberInKey: false, prependParentProjectKey: false, projectKey: '', stashServerBaseUrl: 'http://bitbucket.companyname.com'
				}
				
			}

		}
		catch (err)
		{
			println err
		}
	}	
}




try {
	if (alreadybuilt == 0){
	
		timeout(time: 30, unit: 'MINUTES') {
		
			node(BuildWorkerId) {
			
				stage('Checkout') {
				
					println "building ${CommitID} from ${BRANCH_NAME}"

					git branch: "${BRANCH_NAME}", url: "${repoURL}", changelog: false, credentialsId: 'jenkins_credentials_id', poll: false
				
					//after checkout: defines all variable parameters from services.json and environment.json files (inside `inventory` folder in repo)
					inventory = new Inventory(ppl: this)
					services = inventory.getServices()
					environment = inventory.getEnvironment(env_config)
					project = inventory.getSolutionProject()
					def server_json = environment['application']
					server = new Server(server_json)
					def releaseNumber = inventory.getReleaseNumber(server.config)
					versionApp = "${releaseNumber}.${BUILD_NUMBER}"
					def db_json = environment['db']
					db = new DB(db_json)
					
					//stash powershell script to use it later during service installation:
					dir("${project['installerPath']}") {
						stash includes: "${project['installer']}", name: 'installer'
					}
					
					//notifying bitbucket [in progress]:
					notifyBitbucket commitSha1: CommitID, considerUnstableAsSuccess: false, credentialsId: 'jenkins_credentials_id', disableInprogressNotification: false, ignoreUnverifiedSSLPeer: true, includeBuildNumberInKey: false, prependParentprojectKey: false, projectKey: '', stashServerBaseUrl: 'http://bitbucket.companyname.com'
					
				}
				
				stage('Restore packages') {
					
					nuget_solution("${WORKSPACE}\\Solution\\")
					
				}
				
				stage('Build Client') {
					
					//describing client object with all required parameters:
					Client client = new Client(
						projectFile: project['client']['projectFile'], //'ProjectGui', // project['client']['projectFile'],
						projectFolder: project['client']['projectFolder'], //'Project', // project['client']['projectFolder'],
						buildNumber: versionApp,
						server: server,
						assemblyName: environment['client']['assemblyNameClient'], // 'Test-Project', // environment['client']['assemblyNameClient'],
						productName: environment['client']['productNameClient'] //'Test-Project' //environment['client']['productNameClient']
					)
					
					retry(2) {

						println "Run MSbuild with ${client.server.config} configuration for Project Client, version ${versionApp}"
						//generating MSBuild string to CMD from Builder class:
						def build_string = builder.buildClient(client, env.WORKSPACE)
						bat "${build_string}"
						bat "${build_string}"
					
					}
				}
				
				stage('Build Services') {
				
					//from services.json
					services.each { key, service ->
						retry(2) {
							
							println "Run MSbuild with ${server.config} configuration : ${service.name}"
							
							def ServicePath = "${WORKSPACE}\\build\\${service.name}"
							createFolder(ServicePath)
							cleanFolder(ServicePath)
							
							//generating MSBuild string to CMD from Builder class:
							def build_string = builder.buildApplication(service.name, server.config, env.WORKSPACE, ServicePath)
							bat "${build_string}"
							bat "${build_string}"

							println "End MSbuild with ${server.config} configuration : ${service.name}"
							
						}
						
						//stashing service for further use
						println " = = Stash [ ${service.name}]"
						dir("${WORKSPACE}\\build\\") {
							stash includes: "${service.name}/*", name: service.name
						}
					}
				}
				
				stage('Unit Test') {
					
					def buildUT = builder.buildUnitTest(env.WORKSPACE)
					bat "${buildUT}"
					
					runUnitTest()
				}
				
				stage('Restore from backup and update') {
				
					if (RestoreFromBackupAndUpdate) {
				
						def backup_json = inventory.getBackup(env_config)
						def backupName = "${backup_json['name']}"
						def backupUrl = "${backup_json['locate']}\\$backupName"
						def srcTarget = "${backup_json['path_destination']}"
						
						timeout(time: 25, unit: 'MINUTES') {
							//remove previous backup
							bat "del /S /F /Q $srcTarget\\*"
							//copy new backup
							bat "xcopy $backupUrl $srcTarget /Y /d"
						}
						
						withCredentials([usernamePassword(credentialsId: db.credId, passwordVariable: 'PASS', usernameVariable: 'USERNAME')]) {
							String CRED = "-Dserver=\"${db.host}\" -Ddbname=\"${newDBName}_bak\" -Duser=\"${USERNAME}\" -Dpass=\"${PASS}\""
							dir("${project['dbDir']}") {
								println "Restore"
								bat "gradlew restoreFromUat ${CRED} -Dbackup=\"$srcTarget\\$backupName\""
								bat "gradlew disableTrigger ${CRED}"
								println "Update"
								bat "gradlew update -Dcontext=\"${env_config},user,release,hotfix,change\" ${CRED}"
							}
						}
					}
				}
				
				stage('Run DB changes on clean DB') {
					dir("${inventory.getSolutionProject()['dbDir']}") {
					
						bat "gradlew drop create update -DrunList=\"cleanTest\" -Ddbname=\"${db.name}\""
						
					}
				}
				
			} // end of node statement
			
			node(server.workerId){
				stage("Installing services"){
				
					//`dirService` from environment.json //unstashing installer
					createFolder("${server.dirService}")
					dir("${server.dirService}") {
						unstash 'installer'
					}
					
					//install of services should be wrapped into try/catch block (?) or it should be a few separated try\catches instead of one-big
					services.each { key, service ->
						timeout(time: 4, unit: 'MINUTES') {
							println "Install ${service.name}"
							//`credId` is from `environment.json`, while `withCredentials` is from `Credentials Binding Plugin`,
							//which allows you to use PASS or USERNAME later as environment variables
							withCredentials([usernamePassword(credentialsId: server.credId, passwordVariable: 'PASS', usernameVariable: 'USERNAME')]) {
							
								println "Stop and remove service ${service.name}"
								scriptService('Stop', service.name, server.dirService, USERNAME, PASS)
								scriptService('Remove', service.name, server.dirService, USERNAME, PASS)
								
								createFolder("${server.dirService}\\${service.name}")
								cleanFolder("${server.dirService}\\${service.name}")
								
								dir("${server.dirService}") {
									unstash service.name
								}
								
								println "Setup and start service ${service.name} [credentialsId ${server.credId}]"
								scriptService('Setup', service.name, server.dirService, USERNAME, PASS)
								
							} //end of `withCredentials` statement
						} //end of timeout for `install services`
					} //foreach of services
					
				}
			}
			
			node(BuildWorkerId) {
				stage('Integration Test') {
				
					println "Start test"
					dir('Test') {
						bat "pip install -r requirements.txt"
						bat 'python -m pytest -v -l -s --durations=10'
					}
					
				}
			}
			
		}
		
		currentBuild.result = "SUCCESS"

	}
} catch (err) {
	currentBuild.result = "FAILED"
	println err
} finally {
	
	node(BuildWorkerId){
		println "Current build state is ${currentBuild.result}"
		//notifying bitbucket [with current state]:
		notifyBitbucket commitSha1: CommitID, considerUnstableAsSuccess: false, credentialsId: 'jenkins_credentials_id', disableInprogressNotification: false, ignoreUnverifiedSSLPeer: true, includeBuildNumberInKey: false, prependParentprojectKey: false, projectKey: '', stashServerBaseUrl: 'http://bitbucket.companyname.com'
		
		notifyEmail(currentBuild.result, userEmail)
		
		builder = null
	}
	
	node("master"){
		println currentBuild.result
		if(currentBuild.result == "SUCCESS"){
			try {
				String sh_redis_input_result = sh(script: "/usr/local/bin/redis-cli setex ${JOB_NAME}:${CommitID} 864000 SUCCESS", returnStdout: true).trim()
				println sh_redis_input_result
			}
			catch (err)
			{
				println err
			}
		}
	}
	
	try {
		node(BuildWorkerId){
			
			println "Removing test DB "
			dir("${project['dbDir']}") {

				bat "gradlew drop -Ddbname=\"${newDBName}_bak\""
				
			}
		}
	}
		catch (err)
	{
		println err
	}
	
}


def nuget_solution(String solutionDir) {
  	dir(solutionDir) {
		bat "nuget restore -Verbosity detailed"
	}
}


def cleanFolder(String destPath) {
	def PS = "PowerShell.exe -ExecutionPolicy Bypass -Command"
	def cmdCleanFolder = "${PS} \"Remove-Item ${destPath}\\* -Recurse -Force -Confirm:0\""
	retry(5){
		sleep(1) //if log files were locked by Project service or filebeat
		//bat "rd /S /Q \"${destPath}\""
		//bat returnStatus: true, script: ""
		//bat cmdCleanFolder // just in case
		
		def returnCode1 = bat returnStatus: true, script:  "rd /S /Q \"${destPath}\""
        println "return code is - ${returnCode1}"
        if(returnCode1 == 0 || returnCode1 == 2)
        {
            println "path for removing was deleted or even not exists, continue pipeline..."
        }
        else
        {
            println "there is issues with cleaning of path..."
            currentBuild.result = "FAILED"
			throw new hudson.AbortException("CAN'T REMOVE PATH, SEE DETAILS ABOVE!^")
        }
		
	}
}


def createFolder(String destPath) {
	def PS = "PowerShell.exe -ExecutionPolicy Bypass -Command"
	def cmdCreateFolderIfExist = "${PS} \"if (!\$(Test-Path ${destPath})){ md ${destPath}} \""
	bat cmdCreateFolderIfExist
}


def runUnitTest() {
	def pathVT = 'C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\TestAgent\\Common7\\IDE\\CommonExtensions\\Microsoft\\TestWindow\\'
	withEnv(["VT=$pathVT", "PATH+VT=$pathVT"]) {
		def projectPath = "${WORKSPACE}\\Solution\\UnitTests\\bin\\UnitTests.dll"
		def params = "/logger:Console"
		bat "vstest.console ${projectPath} ${params}"
	}
}


def scriptService(String type, String serviceName, String workDir, String user, String pass) {
	def PS = "PowerShell.exe -ExecutionPolicy Bypass -Command"
	def script = "${workDir}\\services.ps1"
	cmd_command = "${PS} \"${script} -Type ${type} -ServiceName ${serviceName} -WorkDir '${workDir}\\${serviceName}' -ServiceUsername ${user} -ServicePassword ${pass} ;exit \$LASTEXITCODE \""
	if (bat([returnStatus: true, script: cmd_command]) != 0)
		error 'Script Service'
}


def notifyEmail(String status, String email) {
	def summary = ""
	def color = 'SUCCESS' == status ? 'green' : 'red'
	mail from: "JenkinsService",
			to: email,
			subject: "Jenkins: Test Branch ${BRANCH_NAME} Project [${status}]",
			mimeType: "text/html",
			body: """<p>Status Build projects <span  style=\"color:${color}\">${status}</span></p>
    <p>Check console output at &QUOT;<a href='${BUILD_URL}'>${JOB_NAME} [${BUILD_NUMBER}]</a>&QUOT;</p>
    <pre>${summary}</pre>"""
}