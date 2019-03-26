//separate stage should be added - done
		if (jobArgs.isDeployService) {
			node(server.workerId) {
				stage("Preparation of installer"){
					//`dirService` from environment.json
					createFolder("${server.dirService}")
					dir("${server.dirService}") {
						unstash 'installer'
					}
				}
			}// end of node, [need to wrap into stage - done]
				
			//install of services should be wrapped into try/catch block (?) or it should be a few separated try\catches instead of one-big
			SERVICES.each { key, service ->
				timeout(time: 3, unit: 'MINUTES') {
					node(server.workerId){
						stage("Install ${service.name} ") {
							//`credId` is from `environment.json`, while `withCredentials` is from `Credentials Binding Plugin`,
							//which allows you to use PASS or USERNAME later as Environment variables
							withCredentials([usernamePassword(credentialsId: server.credId, passwordVariable: 'PASS', usernameVariable: 'USERNAME')]) {
								logStage("Stop Service ${service.name}")
								scriptService('HardStop', service.name, server.dirService, USERNAME, PASS)
								scriptService('Remove', service.name, server.dirService, USERNAME, PASS)
								createFolder("${server.dirService}\\${service.name}")
								cleanFolder("${server.dirService}\\${service.name}")
								dir("${server.dirService}") {
									unstash service.name
								}
								logStage("Service ${service.name} [credentialsId ${server.credId}]")
								scriptService('Setup', service.name, server.dirService, USERNAME, PASS)
							} //end of `withCredentials` statement
						} //end of stage
					}
				} //end of timeout for `install services`
			} //foreach of services
		} // end of `if 'isDeployService' is true`