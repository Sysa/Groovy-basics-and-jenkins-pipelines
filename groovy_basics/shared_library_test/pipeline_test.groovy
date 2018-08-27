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
