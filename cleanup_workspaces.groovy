import hudson.model.*;
import hudson.util.*;
import jenkins.model.*;
import hudson.FilePath.FileCallable;
import hudson.slaves.OfflineCause;
import hudson.node_monitors.*;

current_job_string = "[" + env.JOB_NAME + " " + env.BUILD_DISPLAY_NAME + "]"
hosts = getProperty('HostsForCleanup')
currentBuild.displayName = currentBuild.number + " " + hosts
hosts = hosts.split("\n")
EmailToReport = getProperty('EmailToReport')
EmailToReport = EmailToReport.split("\n")
email_body = ""
need_to_send_report = false

if(checkForRunningJobs(current_job_string)){
	for (host in hosts)
	{
		stage("cleanup at ${host}"){
			cleanupOnSpecificNode(host)
			}
	}
	email_body += "<br><a href=" + env.JOB_URL + "> Project page in Jenkins </a>"
	if (need_to_send_report == true){
		for(email in EmailToReport)
		{
			//mail body: email_body, from: 'jenkins_cleanup@Project.com', subject: 'Jenkins workspaces Cleanup', to: email
			emailext body: email_body, mimeType: 'text/html', subject: 'Jenkins workspaces Cleanup', to: email
		}
	}
}
else {
	stage("there are running jobs"){
		currentBuild.result = "UNSTABLE"
	}
}

def checkForRunningJobs(current_job_string){
	runningJobs = Jenkins.instance.getView('All').getBuilds().findAll() { it.getResult().equals(null) }
	println "All running jobs " + runningJobs
	//current_job_string = "[" + env.JOB_NAME + " " + env.BUILD_DISPLAY_NAME + "]"
	runningJobs = runningJobs.toString()
	if(runningJobs == current_job_string) {
		println "there is no running jobs"
		return true
	}
	 else {
		println "there are running jobs"
		println runningJobs
		return false
	}
}

def cleanupOnSpecificNode(hostname){

	for (node in Jenkins.instance.nodes) {
		//println "node " + node.name
		if (node.name == hostname){
			computer = node.toComputer()
			if (computer.getChannel() == null) continue

			rootPath = node.getRootPath()
			size = DiskSpaceMonitor.DESCRIPTOR.get(computer).size
			roundedSize = size / (1024 * 1024 * 1024) as int

			println("node: " + node.getDisplayName() + ", free space: " + roundedSize + " GB")
			if (roundedSize < 100) {
				//computer.setTemporarilyOffline(true, new hudson.slaves.OfflineCause.ByCLI("disk cleanup"))
				//println "working on node " + node.name
				
					for (item in Jenkins.instance.items) {
						//jobName = item.getFullDisplayName()

						// if (item.isBuilding()) {
							// println(".. job " + jobName + " is currently running, skipped")
							// continue
						// }

						//println(".. wiping out workspaces of job " + jobName)

						workspacePath = node.getWorkspaceFor(item)
						if (workspacePath == null) {
							println(".... could not get workspace path")
							continue
						}

						//println(".... workspace = " + workspacePath)

						// customWorkspace = item.getCustomWorkspace()
						// if (customWorkspace != null) {
							// workspacePath = node.getRootPath().child(customWorkspace)
							// println(".... custom workspace = " + workspacePath)
						// }

						pathAsString = workspacePath.getRemote()
						if (workspacePath.exists()) {
							workspacePath.deleteRecursive()
							println(".... deleted from location " + pathAsString)
							need_to_send_report = true
							email_body += "Node " + node.name + " - deleted from location " + pathAsString + "<br>"
						} else {
							//println(".... nothing to delete at " + pathAsString)
						}
					}
			// sizeAfterCleanUp = DiskSpaceMonitor.DESCRIPTOR.get(computer).size
			// sizeAfterCleanUp = sizeAfterCleanUp / (1024 * 1024 * 1024) as int
			// println("node: " + node.getDisplayName() + ", free space after cleanup: " + sizeAfterCleanUp + " GB")
			// clearedSize = roundedSize - sizeAfterCleanUp
			// println "cleared " + clearedSize + " GB"
			//email_body += "Node " + node.name + " - cleared " + clearedSize + " GB" + "\n <br>"
				//computer.setTemporarilyOffline(false, null)
			}
		}
	}
}

