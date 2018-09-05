@Library('shared_library_test')_
import org.testorg.test_sources
node('master') {
    stage ("First stage")
        {echo "Hello world"}
    }
stage ("2")
    {echo 'Hello World-2'
        echo env.BUILD_ID
        echo params.test_choise
    }
node {
    stage ("3")
        {
            echo 'Hello-3'
            def choise = getProperty('test_choise')
            echo choise
        }
}
node {
    stage ("shared")
    {
        test_shared_library.call()
    }
}
node {
    stage ("sources")
    {
        test_src = new org.testorg.test_sources()
        test_src.call_test_source()
    }
}

def services = []
services.add("FirstService")
services.add("SecondService")
def list_of_services = ''
node {
	//dynamic name of stage
	services.each{list_of_services += " $it "}
	stage("install of services $list_of_services"){
		sizeOfServices = services.size()
		echo sizeOfServices.toString()
	}
}


node {
	stage ("cleaning workspace")
	{
		echo "Workspace is " + env.WORKSPACE
		//deleteDir()
		sleep(time:3,unit:"SECONDS")
		//this is can be retired with `deleteDir()`
		//clean_workspace(env.WORKSPACE)
		//sleep(time:3,unit:"SECONDS")
		//clean_workspace("${WORKSPACE}")
		echo pwd()
		dir("mytestrepo") {deleteDir()}
		echo pwd()
	}
}

node {
	stage ("git checkout")
	{
		echo "SCM step"
		//http://localhost:7990/scm/tes/mytestrepo.git
		checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
													branches: [[name: '*/testdev']],
													doGenerateSubmoduleConfigurations: false,
													extensions: [], submoduleCfg: [],
													userRemoteConfigs: [[credentialsId: 'alexkh_git_credentials',
													url: 'http://localhost:7990/scm/tes/mytestrepo.git']]]
		echo "Git checkout, shorthand"
		git branch: 'testdev', changelog: false, credentialsId: 'alexkh_git_credentials', poll: false, url: 'http://localhost:7990/scm/tes/mytestrepo.git'
	}
}
node {
	stage ("run job")
	{
		build job: 'simple_job'
	}
}

def new_value = "bin"

node {
	stage ("pass variable")
	{
		pass_string_value(new_value)
	}
}

def pass_string_value (String path_value)
	{
		def parameters = "/p:DefineConstants=DEBUG /p:Platform=\"Any CPU\" /p:OutputPath=$path_value"
		echo parameters
		//the same:
		echo "${WORKSPACE}"
		echo env.WORKSPACE
	}

// node {
	// stage ("sending report"){
		// mail body: 'text',
			// from: 'jenkins@.com',
			// subject: 'subj',
			// to: 'Alex.Kh@.com'
	// }
// }

//this func can be swapped with the `deleteDir()`, but be ahead of this one issue - https://issues.jenkins-ci.org/browse/JENKINS-41805
def clean_workspace(String destination_folder) {
	//def PS = "PowerShell.exe -ExecutionPolicy Bypass -Command"
	//def cmd_command = "${PS} \"if (\$(Test-Path ${destination_folder})) {Remove-Item ${destination_folder}\\* -Recurse -Force\"}"
	//bat cmd_command
	powershell 'write-host "testpowershell" '
}