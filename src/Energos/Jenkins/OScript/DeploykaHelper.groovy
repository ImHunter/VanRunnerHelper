
package Energos.Jenkins.OScript;

class DeploykaHelper extends OScriptHelper {

    String pathToDeployka;
    Map<String, String> params = [:];

    private String KEY_DB_SERVER = 'dbServer';
    private String KEY_DB_DATABASE = 'dbDatabase';
    private String KEY_DB_USER = 'dbUser';
    private String KEY_DB_PWD = 'dbPwd';
    private String KEY_PATH_TO_SERVICE_EPF = 'pathToServiceEpf';

    public DeploykaHelper(Script script, String pathToDeployka, String pathToServiceEPF){
        // super(script);
        this.pathToDeployka = pathToDeployka;
        setParam((KEY_PATH_TO_SERVICE_EPF), pathToServiceEPF);
    }

    def setParam(def paramKey, def paramValue, Boolean isApply = true){
        if (isApply) {
            params.put(paramKey, paramValue);
        };
        params;
    }

    def setParam(Map<String, String> newParams){

    }

    def setDb(String dbServer, String dbDatabase, String dbUser = null, String dbPwd = null) {
        setParam([(KEY_DB_DATABASE):dbDatabase, (KEY_DB_SERVER):dbServer]);
        setParam((KEY_DB_USER), dbUser, dbUser!=null);
        setParam((KEY_DB_PWD), dbPwd, dbPwd!=null);
    }

    def setDbAuth(String dbUser, String dbPwd) {
        setParam([(KEY_DB_USER):dbUser, (KEY_DB_PWD):dbPwd]);
    }

}