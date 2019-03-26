#!groovy
/**
 *
 * Update `hostname` Database on Prod
 *
 */

def HostForPipelineRun = "hostname-01"
def BranchName = getProperty('Branch').toString() // develop; release; etc;
def DatabaseHost = "hostname"
def DBCredentialsID = "hostname_db_admin"
def BackupDBflag = getProperty('BackupDatabase').toBoolean()
 
//gradle properties:
def DatabaseName = "hostname"
def ConfigName = "updateBase"
def ProjectName = "hostname"
//def BackupToPath = "C:\\backup" //path should be available on the `DatabaseHost` server -> liquibase param should be passed: -DbackupPath=\"${BackupToPath}\"
 
node(HostForPipelineRun)
{
	stage("Сheckout")
	{
		git branch: BranchName,
			changelog: false,
			credentialsId: 'GitBot_ssh',
			poll: false,
			url: 'ssh://git@bitbucket.com:7999/common.git'
	}
	
	dir("Database") //enter inside database folder after checkout
	{
		withCredentials([usernamePassword(credentialsId: DBCredentialsID, passwordVariable: 'PASS', usernameVariable: 'USERNAME')]) {
		
			stage("Validate")
			{
				
				bat "gradlew validate --info"

				bat "gradlew status -Dserver=\"${DatabaseHost}\" -Ddbname=\"${DatabaseName}\" -Dconfig=\"${ConfigName}\" -Dproj=\"${ProjectName}\" -Duser=\"${USERNAME}\" -Dpass=\"${PASS}\" --info"
			
			}
			
			stage('Confirmation') {
				//that feeling when confirmation step takes more code than whole pipeline...
				def userInput = false
				try {
					timeout(time: 65, unit: "MINUTES") {
						
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
			
			stage("Backup")
			{
				if (BackupDBflag)
				{
					bat "gradlew backup -Dserver=\"${DatabaseHost}\" -Ddbname=\"${DatabaseName}\" -Duser=\"${USERNAME}\" -Dpass=\"${PASS}\" --info"
				}
				
			}
			
			stage("Update")
			{
				//bat "gradlew create -Ddbname=\"${DatabaseName}\" --info" //in case if there is no DB with such `DatabaseName`
				bat "gradlew update -Dserver=\"${DatabaseHost}\" -Ddbname=\"${DatabaseName}\" -Dconfig=\"${ConfigName}\" -Dproj=\"${ProjectName}\" -Duser=\"${USERNAME}\" -Dpass=\"${PASS}\" --info"
				
			}
		}
	}
}



		
