#!groovy
//name of shared library, can be overwritten per project configuration in pipeline
@Library('sharedlibname')_
// importing sources of package
import com.yourcompany.gitsrc
import com.yourcompany.envn

class Server implements Serializable {
	def host
	def workerId
	def credId
	def dirService
	def config
	def dirClient
}
def definition = [
		job_name: 'deploy_sevices_test_local',
		Status: [SUCCESS: 'SUCCESS', FAILED: 'FAILED', UNSTABLE: 'UNSTABLE'],
		Action  : [remove: 'Remove', deploy: 'Deploy'],
		timeouts: [
				unit    : 'MINUTES',
				checkout: 7,
				build   : 7,
				deploy  : 5
		]]
// defining of pipeline arguments, need to approve signature of `method groovy.lang.GroovyObject getProperty` before use it
// it allows you to get parameters from pipeline of project
def jobArgs = [
		Branch  : getProperty('Branch').toString(),
		EnvironmentName : getProperty('Environment').toString(),
		Services: getProperty('Services').toString(),
		Action  : getProperty('Action').toString(),
]

def subscriptions = []
def workers = [builder: 'master']
def JOB_STATUS = definition.Status.SUCCESS
def Services = null
def PROJECT = null
def ENVIRONMENT = null
Inventory inventory = null
Server server = null
node() {
	stage('Checkout source') {
		println "Checkout source"
		new gitsrc(ppl: this, url: 'http://localhost:7990/scm/test/local_copy_obfuscated.git')
		println "Read Inventory"
		inventory = new Inventory(ppl: this)
		//gets Services from `\Inventory\Services.json`
		Services = inventory.getServices()
		//gets `environment` params from pipeline configuration
		ENVIRONMENT = inventory.getEnvironment(jobArgs.EnvironmentName)
		//gets `project` section from `\Inventory\environment.json`
		PROJECT = inventory.getSolutionProject()

		//gets `application` section from `\Inventory\environment.json`
		//and filling with server-params into the `server`
		def server_json = ENVIRONMENT['application']
		server = new Server(server_json)

		//is used only for `Client` project?
		println "Stash installer"
		dir("${PROJECT['installerPath']}") {
			//stash includes: "${PROJECT['installer']}", name: 'installer'
		}
	}
} // end of first `node` section
def label = ''
def targetServices = [:]
def list_of_Services = ''
//Services from `\Inventory\Services.json` file
println "Services in inventory file: $Services "

if (getProperty('AllServices').toBoolean() == true) {
	targetServices = Services
	lable = '> All Services'
	println "ALL Services"
} else {
	println "NOT ALL Services"
	Services.each{ key, service ->
		currServiceName = service.name
		Servicestatement = getProperty("$currServiceName").toBoolean()
		if (Servicestatement == true){
				println "SELECTED SERVICE : " + currServiceName
				targetServices[currServiceName] = Services[currServiceName]
			}
	}
} 
println "TARGET Services : $targetServices"

// currentBuild - The currentBuild variable may be used to refer to the currently running build.
// It has the following readable properties, such as `description` or `displayName.
// More properties of `currentBuild` can be found at pipeline-syntax/globals page
def descriptionCustom = "<a href=\"${env.BUILD_URL}\">${jobArgs.EnvironmentName}${label}</a>"
currentBuild.description = descriptionCustom

try {
	//this condition will cover case when no one service was selected:
	if (targetServices == [:]){
		stage("No Services selected"){
			echo "No Services selected"
			currentBuild.result = "UNSTABLE"
		}
	}
	else 
	{
		timeout(time: 30, unit: 'MINUTES') 
		{
			//if Action is not `remove`, then build
			if (jobArgs.Action != definition.Action.remove) {
				node() {
					stage('Build') {
						println "Build"
						//WORKSPACE is global variable of current pipeline
						def BUILD_PATH = "${WORKSPACE}\\build\\"
						cleanFolder(BUILD_PATH)
						//nuget_solution("${WORKSPACE}\\Solution\\")
						targetServices.each { key, service ->
								ToBuild(service.name, server.config)
							dir(BUILD_PATH) {
								//stash includes: "${service.name}/*", name: service.name
							}
						}
					}
				} // end of build stage
				//setup type depends on Action type into build parameters:
				String setupType = jobArgs.Action == definition.Action.deploy ? 'Setup' : 'Start'
				node() 
				{
					createFolder("${server.dirService}")
					dir("${server.dirService}") {
						//unstash 'installer'
					}
					//generating dynamic name of the stage
					
					targetServices.each{ key, service -> list_of_Services += " $service.name "}
					stage ("${jobArgs.Action} of service(s) $list_of_Services") {
						println "----- stage of installing $list_of_Services -------"
						targetServices.each { key, service ->
							timeout(time: definition.timeouts.deploy, unit: 'MINUTES') {
								println "-----install of service $service -----"						 
								// withCredentials([usernamePassword(credentialsId: server.credId, passwordVariable: 'PASS', usernameVariable: 'USERNAME')]) {
								//
								// } //end of withCredentials block
							} //end of timeout
						} //end of targetServices foreach statement
					} //end of stage publish of Services
				} //end of node of publush stage
			} //end of if with publish, deploy Actions
				if (jobArgs.Action == definition.Action.remove) {
					node() {
						targetServices.each{ key, service -> list_of_Services += " $service.name "}
						stage("${jobArgs.Action} of service(s) $list_of_Services") {
							targetServices.each { key, service -> 
								timeout(time: definition.timeouts.deploy, unit: 'MINUTES') 
								{
									println "Remove Service ${service.name}"
									//scriptService('Remove', service.name, server.dirService)
								}
							}
						}
					}
				} //end of remove Action
		} //end of timeout
	}
} catch (err) {
	println err
	JOB_STATUS = definition.Status.FAILED
	currentBuild.result = JOB_STATUS

} finally {
	for (email in subscriptions) {
		//
	}
}

// can be moved to common library (the same for Cabinet, but they are different. need to be checked first
def ToBuild(String appName, String config) {
	// if (config.contains('Test')) {
		// scriptSetProperty("${WORKSPACE}\\Solution\\${appName}\\")
	// }
	// def defConst = ""
	// if (config == 'Test' || config == 'Debug' || config == 'Dev') {
		// defConst = "/p:DefineConstants=DEBUG"
	// }
	// def path = "${WORKSPACE}\\build\\${appName}"
	// createFolder(path)
	// cleanFolder(path)
	// def projectPath = "${WORKSPACE}\\Solution\\${appName}\\${appName}.csproj"
	// def target = "/t:Clean,Build"
	// def configuration = "/p:Configuration=${config}"
	// def solutionDir = "/p:SolutionDir=${WORKSPACE}\\Solution"
	// def VSToolsPath = "/p:VSToolsPath=c:\\MSBuild.Microsoft.VisualStudio.Web.targets.\\tools\\VSToolsPath"
	// def platform = "/p:Platform=\"Any CPU\""
	// def outPath = "/p:OutputPath=${path}"
	// def verbosity = "/verbosity:minimal"
	// bat "MSBuild ${projectPath} ${target}  ${platform} ${defConst} ${outPath} ${VSToolsPath} ${solutionDir} ${configuration} $verbosity"
}
def scriptService(String type, String serviceName, String workDir, String user = '', String pass = '') {
	// def script = "${workDir}\\install-service.ps1"
	// def status = powershell(returnStatus: true,
			// script: "${script} -Type ${type} -ServiceName ${serviceName} -WorkDir '${workDir}\\${serviceName}' -ServiceUsername '${user}' -ServicePassword '${pass}'")
	// if (status != 0) error "Script Service [$status]"
}

// can be moved to the common library:
def nuget_solution(String solutionDir) {
  	// dir("${solutionDir}") {
		// bat "nuget restore -Verbosity detailed"
    // }
}
// can be moved to the common library or even replaced with deleteDir() and dir('folder') 
// [dir step, if the directory doesn't exist, then the dir step will create the folders needed once you write a file or similar:]: dir ('foo') {writeFile file:'file', text:''}
def cleanFolder(String destPath) {
	def PS = "PowerShell.exe -ExecutionPolicy Bypass -Command"
	def cmdCleanFolder = "${PS} \"if (\$(Test-Path ${destPath})) {Remove-Item ${destPath}\\* -Recurse -Confirm:0\"}"
	bat cmdCleanFolder
}
def createFolder(String destPath) {
	def PS = "PowerShell.exe -ExecutionPolicy Bypass -Command"
	def cmdCreateFolderIfExist = "${PS} \"if (!\$(Test-Path ${destPath})){ md ${destPath}} \""
	bat cmdCreateFolderIfExist
}
