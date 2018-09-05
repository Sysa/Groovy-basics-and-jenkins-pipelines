import hudson.FilePath
import jenkins.model.Jenkins
import hudson.model.Job
import java.io.File;


def reportUnusedWorkspace(FilePath root, String path) {
  root.list().sort{child->child.name}.each { child ->
    String fullName = path + child.name
	println " --------- start ---------- "
	println "child is : " + child
    def item = Jenkins.instance.getItemByFullName(fullName);
    println "Checking '$fullName'"
	
	//if(child.toString().contains(".git")){
		//println "this is a git directory, skipping"
		//}
	
	if(child.toString().contains(".git")){
			println "this is a git directory, skipping"
			return false
			}
	try {
		println "canonicalName is : " + item.class.canonicalName
		}
	catch (Exception e)
	{
		// if(child.toString().contains(".git")){
			// println "this is a git directory, skipping"
			// return false
			// }
		// else{println "exception during canonicalName : " + e}
		println "exception during canonicalName : " + e
	}
	
	
	
    try{
		//need to check also for:
			//hudson.model.FreeStyleProject
			//org.jenkinsci.plugins.workflow.job.WorkflowJob
			
		//also need to check for already empty directories to avoid temp folder removing
			//directorySize()
			//println new File('.').directorySize() - size of whole pipeline
			//println new File("C:/logs").directorySize()
		try{
			//.getAbsolutePath()
			//child.replaceAll("\\\\", "/")
			//println "getting the abs path " + child.absolutize()
			
			println "list the directory " + child.list()
			if (child.list() == [])
				{
					println "this is an empty directory -> " + child
				}
			
			//println new File("$child").directorySize()
			
			}
		catch(Exception e)
			{println "Directory listing issues " + e}
		//
		// Jenkins.instance.getAllItems(AbstractProject.class).each {it -> println it.fullName;}
		//
		// jobs = hudson.model.Hudson.instance.getAllItems(FreeStyleProject)
		// for (job in jobs) { println "job -> " + job }
		//
		
      if (item.class.canonicalName == 'com.cloudbees.hudson.plugins.folder.Folder' ||
			item.class.canonicalName == 'hudson.model.FreeStyleProject' ||
			item.class.canonicalName == 'org.jenkinsci.plugins.workflow.job.WorkflowJob') {
		println "this is an object in Jenkins pipeline"
        println "-> going deeper into the folder"
		reportUnusedWorkspace(root.child(child.name), "$fullName/")
		//return false
      } else if (item == null) {
        // this code is never reached, non-existing projects generate an exception
        println "Deleting (no such job): '$fullName'"
        //child.deleteRecursive()
      } else if (item instanceof Job && !item.isBuildable()) {
        // don't remove the workspace for disabled jobs!
        //println "Deleting (job disabled): '$fullName'"
        //child.deleteRecursive()
      }
    } catch (Exception exc) {
		println "   Exception happened: " + exc.message
		println "   So we delete '" + child + "'!"
      //child.deleteRecursive()
    }
  }
  println " --------- end ---------- "
}

println "Beginning of cleanup script."

// loop over possible slaves
for (node in Jenkins.instance.nodes) {
  println "Processing $node.displayName"
  println node
  println node.name
  println node.remoteFS
  
  	println "----------------"
	
	node.properties.each { k,v -> println k + "-> " + v}
	println "node channel " + node.properties.channel
	//Jenkins.getInstance().getComputer(env.NODE_NAME).getNode()
	println "node label is " + node.properties.selfLabel
	try {
		//def nodeName = Jenkins.getInstance().getComputer(node.properties.selfLabel).getNode()
		//def nodeName = getComputer(node.properties.selfLabel.toString())
		//this is hostname
		def nodeName = Jenkins.getInstance().getComputer(node.properties.selfLabel.toString()).getHostName()
		println nodeName
		}
	catch (Exception e)
		{println "error -> " + e}
	
	
	println "----------------"
  
  for (items in node){
	println items
	}
  
  def workspaceRoot = node.rootPath.child("workspace");
  if(node.displayName == "jnktest-04"){
	reportUnusedWorkspace(workspaceRoot, "")
	}
}


// do the master itself
//println "Processing master node"
//reportUnusedWorkspace(Jenkins.instance.rootPath.child("workspace"), "")

println "Script has completed."
