# Some Groovy scripts and few jenkins pipelines
All scripts has been written by me

## Gradle_runner
Gradle + Liquibase pipelines to restore/drop/update or even create database via Jenkins or any local CI engine

## Jenkins_shared_library_test
Skeleton of shared library in Jenkins

## Other `.groovy` scripts:
**Builder** - simple wrapper over MSBuild \ MSDeploy commands

**Jenkinsfile** - example of jenkinsfile for some project

**Jenkinsfile_ssh_unit_tests** - example of pipeline without using Jenkins agent, only bunch of commands and output

**cleanup_workspaces** - Jenkins script to cleanup workspaces across the nodes, including master

**test_pipeline** - another example of pipeline with standard steps

**useful_tips_and_cases**:
- credentials - how to show jenkins credentials if you are not aware of it
- confirmation check - how to pause pipeline execution until confirmation
-  bat handle return status - how some `bat` statuses can be handled
-  parse git commands, regex - how to parse commands results using regex (git command as example)
-  jenkins API call example - how to call Jenkins API using Jenkins pipeline
-  parallel test - example of parallel run of tests

**in_process_script_approval.txt** - how to approve signatures, if really required
