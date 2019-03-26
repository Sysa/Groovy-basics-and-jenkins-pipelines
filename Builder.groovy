package com.sovacapital

class Builder {

	def buildApplication(String appName, String config, String WorkspacePath, String ServicePath) {
	
		def defConst = ""
		def verbosity = ""

		if (config == 'Test' || config == 'Debug' || config == 'Dev') {
			defConst = "/p:DefineConstants=DEBUG"
			verbosity = "/verbosity:minimal"
		}

		def projectPath = "${WorkspacePath}\\Solution\\${appName}\\${appName}.csproj"
		def configuration = "/p:Configuration=${config}"
		def solutionDir = "/p:SolutionDir=${WorkspacePath}\\Solution"
		def outPath = "/p:OutputPath=${ServicePath}"
		def target = "/t:Clean,Build"
		def VSToolsPath = "/p:VSToolsPath=c:\\MSBuild.Microsoft.VisualStudio.Web.targets.14.0.0.3\\tools\\VSToolsPath"
		def platform = "/p:Platform=\"Any CPU\""

		GString result = "MSBuild ${projectPath} ${target} ${platform} ${defConst} ${outPath} ${VSToolsPath} ${solutionDir} ${configuration} ${verbosity}"
		
		return result
	}
	
	
	def buildUnitTest(String WorkspacePath)	{
		def projectPath = "${WorkspacePath}\\Solution\\UnitTests\\UnitTests.csproj"
		def outPath = "/p:OutputPath=bin"
		def solutionDir = "/p:SolutionDir=${WorkspacePath}\\Solution"
		def VSToolsPath = "/p:VSToolsPath=c:\\MSBuild.Microsoft.VisualStudio.Web.targets.14.0.0.3\\tools\\VSToolsPath"
		def platform = "/p:Platform=\"Any CPU\""
		def verbosity = "/verbosity:quiet"
		
		GString result = "MSBuild ${projectPath}  ${platform} ${outPath} ${VSToolsPath} ${solutionDir} $verbosity"
		
		return result
	}
	
	def buildClient(client, WorkspacePath) {
		def appName = client.projectFile
		def appNameDir = client.projectFolder

		def DefineConstants = ""
		if (client.server.config == 'Test' || client.server.config == 'Debug' || client.server.config == 'Dev') {
			DefineConstants = "/p:DefineConstants=DEBUG"
		}
		
		//AdditionalParams must be set to avoid situations when run of `UAT` or `DEV` or `PROD` will replace each other (project gui client)
		//ProjectIcon will set different icons for different environments
		def AdditionalParams = ""
		if (client.server.config == 'Dev')
		{
			AdditionalParams = "/p:MSBuildProductName=ProjectDev /p:MSBuildAssemblyName=Project.dev /p:ProjectIcon=dev"
		}
		if (client.server.config == 'UAT')
		{
			AdditionalParams = "/p:MSBuildProductName=ProjectUAT /p:MSBuildAssemblyName=Project.uat /p:ProjectIcon=uat"
		}

		def value = client.server.dirClient.tokenize('\\')[1]
		String destUrl = "\\\\${client.server.host}.otkritie.local\\${value}\\"

		String PublishDirPath = "\\\\${client.server.host}\\C\$\\${value}\\"
		String projectPath = "${WorkspacePath}\\Solution\\${appNameDir}\\${appName}.csproj"
		String solutionDir = "/p:SolutionDir=${WorkspacePath}\\Solution"

		String conf = "/p:Configuration=${client.server.config}"

		String installURL = "/p:InstallURL=${destUrl}"
		String publishDir = "/p:PublishDir=${PublishDirPath}"
		String appVer = "/p:ApplicationVersion=${client.buildNumber}"

		String target = "/t:Publish"
		String platform = "/p:Platform=\"Any CPU\""
		String outPath = "/p:OutputPath=bin"
		String extParams = "/p:BootstrapperEnabled=true /p:GenerateManifests=true /p:DeployOnBuild=True"
		String VSToolsPath = "/p:VSToolsPath=c:\\MSBuild.Microsoft.VisualStudio.Web.targets.14.0.0.3\\tools\\VSToolsPath"
		

		GString result = "MSBuild ${projectPath} ${target} ${platform} ${outPath} ${VSToolsPath} ${solutionDir} ${conf} ${installURL} ${publishDir} ${appVer} ${extParams} ${DefineConstants} ${AdditionalParams}"
		
		return result
	}

}
