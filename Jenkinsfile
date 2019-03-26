#!/usr/bin/env groovy
// put #!/usr/bin/env groovy at the top of the file so that IDEs, GitHub diffs, etc properly detect the language and do syntax highlighting for you.

//pipeline parameters:
def nodeID = "HOSTNAME" //main node to execute pipeline //
def GitVariables = "" //defining variable
def repoURL = "ssh://git@bitbucket.com:7999/project.git" //repository url


//for copy DB backup:
def backup_source= "\\\\hostname\\SQLBackup"
def backup_destination = "C:\\Backups"
def dbName = "Project"

node(nodeID)
{
	try{
	
		// /////////////
		// //this should be uncommented in case of NOT Multibranch pipeline:
		// properties([parameters([string(defaultValue: "AUT-123-creating-CI-workflow", description: 'Branch name', name: 'branchName')])])
		// BRANCH_NAME = getProperty('branchName').toString()
		// println BRANCH_NAME
		// /////////////
		
		// This limits build concurrency to 1 per branch
		properties([disableConcurrentBuilds()])
		
		println "CURRNET BRANCH NAME "
		println BRANCH_NAME
		
		
		stage("Checkout")
		{
			GitVariables = git branch: "${BRANCH_NAME}", url: "${repoURL}", changelog: false, credentialsId: 'Jenkins-credentials-hash-credentials-id', poll: false
			println GitVariables.GIT_COMMIT
			
			//notifying bitbucket:
			//currentBuild.result = 'INPROGRESS'
			notifyBitbucket commitSha1: GitVariables.GIT_COMMIT, considerUnstableAsSuccess: false, credentialsId: 'Jenkins-credentials-hash-credentials-id', disableInprogressNotification: false, ignoreUnverifiedSSLPeer: true, includeBuildNumberInKey: false, prependParentProjectKey: false, projectKey: '', stashServerBaseUrl: 'http://bitbucket.com'
		}
		
		
		stage("Copy DB backup")
		{
			bat "xcopy ${backup_source}\\${dbName}.bak ${backup_destination} /Y /d"			
		}
		
		
		stage("Unit Test")
		{
			dir("Solution")
			{
				bat "dotnet test --logger:\"console;verbosity=detailed\""
				//bat "dotnet test"
				// https://xunit.github.io ?
				// --logger:"console;verbosity=normal"
				// --logger:"console;verbosity=detailed"
			}
		}
		

		stage("External Api Restore")
		{
			
			//StepPBExternalApiRestoreDebug:
			bat "dotnet.exe publish Solution\\API\\API.csproj --framework netcoreapp2.1 --configuration Test --runtime win10-x64 --output C:\\dev\\API"
			///p:AssemblyVersion=02.26.0.44 /p:FileVersion=02.26.0.44 /p:Version=02.26.0.44

		}
		
		stage("External Api Publish"){
		
			//StepPBExternalApiPublish:
			bat "\"C:\\Program Files (x86)\\IIS\\Microsoft Web Deploy V3\\msdeploy.exe\" -verb:sync -source:iisapp='C:\\API' -dest:iisApp='Default/API',computerName='https://hostname:8172/msdeploy.axd?site=Default',username=\"hostname\\webdeployaccount\",password=\"pa\$\$word!\",authtype=\"Basic\",includeAcls=\"False\" -disableLink:AppPoolExtension -disableLink:ContentExtension -disableLink:CertificateExtension -enableRule:AppOffline -allowUntrusted"
		
		}
		
		stage("External Frontend Build"){
		
			//StepPBExternalFrontendBuildDebug:
			dir("Solution/Frontend"){
				bat "npm cache clean --force"
				bat "npm config set cache ${WORKSPACE}\\npm-cache"
				bat "npm install"
				bat "npm run ng run ClientApp:build:test"
			}
			
		}
		
		
		stage("External Frontend Publish"){
			//StepPBExternalFrontendPublishDebug:
			dir("Solution/Frontend"){
				bat "xcopy wwwroot\\dist\\ClientApp\\* C:\\Frontend /f /y"
			}
		}
		
		
		stage("External Frontend Deploy"){
			
			//StepPBExternalFrontendDeployDebug:
			bat "\"C:\\Program Files (x86)\\IIS\\Microsoft Web Deploy V3\\msdeploy.exe\" -verb:sync -source:iisapp='C:\\Frontend' -dest:iisApp='Default/Frontend',computerName='https://hostname:8172/msdeploy.axd?site=Default',username=\"hostname\\webdeployaccount\",password=\"pa\$\$word\",authtype=\"Basic\",includeAcls=\"False\" -disableLink:AppPoolExtension -disableLink:ContentExtension -disableLink:CertificateExtension -enableRule:AppOffline -allowUntrusted"
			
		}
		
		
		stage("Install DB"){
			
			dir('Database'){

				//cleanup (liquibase `drop` task in build.gradle):
				bat "gradlew drop -Ddbname=\"Project\" --info"
				
				//restore from backup (liquibase `restore` task in build.gradle):
				bat "gradlew restore -Ddbname=\"Project\" --info"
				
				bat "gradlew update -DrunListParam=\"update_Project\" -Dcontext=\"autotest\" --info"
				
			}
			
		}
		
		stage("Application pool recycle"){
		
			bat "appcmd stop apppool \"Project\""
			bat "appcmd start apppool \"Project\""
		
		}
		

		// stage("Integration Test")
		// {
			// //to be continued
		// }
		
		// stage("Functional Test (skip)")
		// {
			// //to be continued
		// }
		
		
		//notifying bitbucket:
		currentBuild.result = 'SUCCESS'
		notifyBitbucket commitSha1: GitVariables.GIT_COMMIT, considerUnstableAsSuccess: false, credentialsId: 'credentials-id-hash', disableInprogressNotification: false, ignoreUnverifiedSSLPeer: true, includeBuildNumberInKey: false, prependParentProjectKey: false, projectKey: '', stashServerBaseUrl: 'http://bitbucket.com'
		
	
	}
	catch (exc)
	{
		println exc
		//notifying bitbucket:
		currentBuild.result = 'FAILED'
		notifyBitbucket commitSha1: GitVariables.GIT_COMMIT, considerUnstableAsSuccess: false, credentialsId: 'credentials-id-hash', disableInprogressNotification: false, ignoreUnverifiedSSLPeer: true, includeBuildNumberInKey: false, prependParentProjectKey: false, projectKey: '', stashServerBaseUrl: 'http://bitbucket.com'
	}
	finally {

		println "Current build state:"
		println currentBuild.result
		println "cleaning workspace directory"
		deleteDir()

	}
}

