//this is for https://stackoverflow.com/questions/52161880/how-to-stop-the-sending-email-from-jenkins-pipeline-when-cancel-the-job-in-jenki/52253524#52253524
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException
node(){
    stage("doing things"){
        sendEmailflag=true
        try{
            echo "into try block"
            sleep 10
        }
        catch(org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
            sendEmailflag = false
            echo "!!!caused error $e"
            throw e
        }
        finally{
            if(sendEmailflag == false)
                {echo "do not send e-mail"}
            else
                {echo "send e-mail"}
        }
    }
}