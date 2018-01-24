import Energos.Jenkins.OScript.DeploykaHelper;

def call(def pathToDeployka, def pathToServiceEPF = null){

    new DeploykaHelper(this, pathToDeployka.toString(), pathToServiceEPF==null ? null : pathToServiceEPF.toString());

}