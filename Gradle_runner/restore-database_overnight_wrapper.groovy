// wrapper for restore-database script, schedule:
// build periodically with parameters:
// H 2 * * * % Environment=TEST;
// H 3 * * * % Environment=UAT;

node("master"){
    stage("git remote"){
        withCredentials([usernamePassword(credentialsId: 'Credentials-ID-In-Jenkins', passwordVariable: 'PASS', usernameVariable: 'USERNAME')]) {
			try {
				String filterBranch = "release/"
				String sh_git_result = sh(script: "git ls-remote http://${USERNAME}:${PASS}@bitbucket.com:7999/project.git | grep ${filterBranch}", returnStdout: true).trim()
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
	
	stage("executing job"){
	
		//try call job with last successful branch parameter, otherwise call for it with `develop` branch
		
		try {
			if(branch_result){
				build(job: 'Project/restore_database',
					parameters: [
						string(name: 'Environment', value: 'UAT'),
						string(name: 'DBContext', value: 'uat'),
						string(name: 'Branch', value: branch_result)])
					
						//other parameters remains as default
			}
		}
		catch (err2) {
			
			println err2
			println "Trying develop branch"
			
			try {
				
				build(job: 'Project/restore_database',
					parameters: [
						string(name: 'Environment', value: 'UAT'),
						string(name: 'DBContext', value: 'uat'),
						string(name: 'Branch', value: 'develop')])
				
			}
			catch (error) {
				println error
			}
			
		}
	
	}
}