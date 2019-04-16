import Energos.Jenkins.OScript.JenkinsBatchExecuter

def call(def pathToScript = null, def pathToServiceEPF = null){

    new JenkinsBatchExecuter(this)

}
