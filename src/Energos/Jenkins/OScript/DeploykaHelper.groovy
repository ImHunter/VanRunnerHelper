
package Energos.Jenkins.OScript;

class DeploykaHelper extends OScriptHelper {

    String pathToDeployka;
    Map<String, String> params = [:];

    private String KEY_DB_SERVER = 'dbServer';
    private String KEY_DB_DATABASE = 'dbDatabase';
    private String KEY_DB_USER = 'dbUser';
    private String KEY_DB_PWD = 'dbPwd';
    private String KEY_PATH_TO_SERVICE_EPF = 'pathToServiceEpf';

    private String KEY_REPO_PATH = 'repoPath';
    private String KEY_REPO_USER = 'repoUser';
    private String KEY_REPO_PWD = 'repoPwd';


    public DeploykaHelper(def paramScript, String pathToDeployka, String pathToServiceEPF = null){
        super(paramScript); 
        this.pathToDeployka = pathToDeployka;
        setParam((KEY_PATH_TO_SERVICE_EPF), pathToServiceEPF, pathToServiceEPF!=null);
    }

    @NonCPS
    def setParam(String paramKey, String paramValue, Boolean isApply = true){
        if (isApply) {
            params.put(paramKey, paramValue);
        };
        params;
    }

    def setParam(Map<String, String> newParams, isIgnoreEmptyValues = true){
        def filtered;
        if (isIgnoreEmptyValues) {
            filtered = newParams.findAll { it.value != null }
        } else {
            filtered = newParams;
        }
        params << filtered;
        params;
    }

    def setDb(String dbServer, String dbDatabase, String dbUser = null, String dbPwd = null) {
        setParam([(KEY_DB_DATABASE):dbDatabase, (KEY_DB_SERVER):dbServer, (KEY_DB_USER):dbUser, (KEY_DB_PWD), dbPwd]);
        // setParam((KEY_DB_USER), dbUser, dbUser!=null);
        // setParam((KEY_DB_PWD), dbPwd, dbPwd!=null);
    }

    def setDbAuth(String dbUser, String dbPwd) {
        setParam([(KEY_DB_USER):dbUser, (KEY_DB_PWD):dbPwd]);
    }

    def setRepo(String repoPath, String repoUser = null, String repoPwd = null) {
        // setParam()
        setParam([(KEY_DB_DATABASE):dbDatabase, (KEY_DB_SERVER):dbServer]);
        setParam((KEY_DB_USER), dbUser, dbUser!=null);
        setParam((KEY_DB_PWD), dbPwd, dbPwd!=null);
    }

    def setDbAuth(String dbUser, String dbPwd) {
        setParam([(KEY_DB_USER):dbUser, (KEY_DB_PWD):dbPwd]);
    }

}