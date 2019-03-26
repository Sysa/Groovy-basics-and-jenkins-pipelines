#!/usr/bin/env groovy
// put #!/usr/bin/env groovy at the top of the file so that IDEs, GitHub diffs, etc properly detect the language and do syntax highlighting for you.

def nodeID = "JenkinsHostName" //main node to execute pipeline
def repoURL = "ssh://git@bitbucket.com:7999/project.git" //repository url
def branchName = getProperty('Branch').toString() // develop; release; etc;
def serverParam = getProperty('Environment').toString() // uat / test / prod / dev
def DBcredentials = "cassaddbadmin" // default DB credentials for JenkinsHostName
def DBContext = getProperty('DBContext').toString()

//for copy DB backup:
def backup_source= "\\\\hostname\\SQLBackup\\"
def backup_destination = "C:\\Backups\\"
def dbName = "Project"

//ReportToEmail:
ReportToEmail = getProperty('ReportToEmail').tokenize('\n')
JOB_STATUS = "SUCCESS" //used just to colorize email report

currentBuild.displayName = currentBuild.number + " " + serverParam + " " + branchName


node(nodeID)
{
	try {
	
		if (serverParam == "UAT" || serverParam == "TEST"){
			DBcredentials = "test_db_admin" // for UAT or TEST database; in MSSQL it has name `maintenance-user`
		}
	
		stage("Checkout")
		{
			
			GitVariables = git branch: "${branchName}", url: "${repoURL}", changelog: false, credentialsId: 'HASH-ID-FROM-JENKINS-CREDENTIALS', poll: false
			println GitVariables.GIT_COMMIT
			
		}
		
		node(serverParam){
			stage("Copy DB backup") {
				bat "xcopy ${backup_source}\\${dbName}.bak ${backup_destination} /Y /d"
			}
		}
		
		stage("Restore Database"){
			
			dir('Database'){
				
				withCredentials([usernamePassword(credentialsId: DBcredentials, passwordVariable: 'PASS', usernameVariable: 'USERNAME')])
				{
					// bat "gradlew validate"
					// bat "gradlew status"
					bat "gradlew drop restore update -Dserver=\"${serverParam}\" -Ddbname=\"${dbName}\" -Dcontext=\"${DBContext}\" -DbackupPath=\"${backup_destination}${dbName}.bak\" -Duser=\"${USERNAME}\" -Dpass=\"${PASS}\" --info"
					
				}
				
			}
			
		}
		
	
	} catch (Err) {
		
		JOB_STATUS = "FAILED"
		println Err
		
	}
	finally {
	
	for (email in ReportToEmail) {
		notifyEmail(email, JOB_STATUS, serverParam, dbName)
	}
	
	println "cleaning workspace directory"
	deleteDir()
	
	}
	
}

def notifyEmail(String email, String status, String EnvName, String dbName) {
	def color = 'SUCCESS' == status ? 'green' : 'red'
	mail from: "JenkinsService",
			to: email,
			subject: "${dbName} - ${EnvName}: Database recovery [${status}]",
			mimeType: 'text/html',
			body: """<style>p {font-family: Arial, Helvetica, sans-serif; }</style>
			<p>Recovery database <b>${dbName}</b> at ${EnvName} <span  style=\"color:${color}\"><b>${status}</b></span></p>
    <p>Pipeline results: &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>
				"""
}