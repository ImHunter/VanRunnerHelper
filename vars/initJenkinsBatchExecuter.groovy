import Energos.Jenkins.OScript.JenkinsBatchExecuter

def call(){

    final def executer = new JenkinsBatchExecuter(this)
    executer

}
