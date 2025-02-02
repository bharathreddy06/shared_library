def call(String registryCred = 'a', String registryin = 'a', String docTag = 'a', String grepo = 'a', String gbranch = 'a', String gitcred = 'a') {

pipeline {
environment { 
		registryCredential = "${registryCred}"
		registry = "$registryin" 	
		dockerTag = "${docTag}$BUILD_NUMBER"
		gitRepo = "${grepo}"
		gitBranch = "${gbranch}"
		gitCredId = "${gitcred}"
	}
		
	agent none
	
	stages {
		stage("POLL SCM"){
      agent{label 'docker'}
			steps {
				 checkout([$class: 'GitSCM', branches: [[name: "$gitBranch"]], extensions: [], userRemoteConfigs: [[credentialsId: "$gitCredId", url: "$gitRepo"]]])
			}
		}	
					
		stage('BUILD IMAGE') {
       agent{label 'docker'}
			 steps { 
				 script { 
					 dockerimage = docker.build registry + ":$dockerTag" 
				 }
			} 
		}
					
		stage('PUSH HUB') { 
       agent{label 'docker'}
			 steps { 
				 script {
					 docker.withRegistry( '', registryCredential ) { 
			                        dockerimage.push() 
                    			}
                		}		
			} 
		}
					
		stage('DEPLOY IMAGE') {
      agent{label 'kubernetes'}
			steps {
				sh 'kubectl set image deployment/webapp-deployment nodejs="$registry:$dockerTag" --record'
			}
		}
	}
			  
}

}
