
package Energos.Jenkins.OScript;

class DeploykaHelper extends OScriptHelper {

    String pathToDeployka;
    String pathToServiceEPF;

    DeploykaHelper(Script script, String pathToDeployka, String pathToServiceEPF){
        super(script);
        this.pathToDeployka = pathToDeployka;
        this.pathToServiceEPF = pathToServiceEPF;
    }

}