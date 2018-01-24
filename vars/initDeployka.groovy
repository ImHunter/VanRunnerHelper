import Energos.Jenkins.OScript.DeploykaHelper;

def call(def pathToDeployka, def pathToServiceEPF){

    new DeploykaHelper(this, pathToDeployka.toString(), pathToServiceEPF.toString());

}