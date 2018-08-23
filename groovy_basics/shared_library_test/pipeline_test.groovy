@Library('shared_library_test')_
import org.testorg.test_sources
node {
    stage ("First stage")
        {echo "Hello world"}
    }
stage ("2")
    {echo 'Hello World-2'}
node {
    stage ("3")
        {echo 'Hello-3'}
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