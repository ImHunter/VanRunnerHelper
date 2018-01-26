
package Energos.Jenkins.OScript;

// import Energos.Jenkins.OScript.LockResourcesEnum;

class DeploykaHelper extends OScriptHelper {

    String pathToDeployka;
    Map<String, String> params = [:];
    String ucCode = 'blocked';

    private String KEY_DB_SERVER = 'dbServer';
    private String KEY_DB_DATABASE = 'dbDatabase';
    private String KEY_DB_USER = 'dbUser';
    private String KEY_DB_PWD = 'dbPwd';
    private String KEY_PATH_TO_SERVICE_EPF = 'pathToServiceEpf';

    private String KEY_REPO_PATH = 'repoPath';
    private String KEY_REPO_USER = 'repoUser';
    private String KEY_REPO_PWD = 'repoPwd';

    private String KEY_RAS_SERVER = 'ras_server';
    private String KEY_RAC_UTIL_PATH = 'rac_util_path';

    private String connString;

    enum DeplCommand {
        dcRun {
            @NonCPS
            @Override
            public String toString() {return "run";}
        },
        dcLoadCfg {
            @NonCPS
            @Override
            public String toString() {return "loadcfg";}
        },
        dcSession {
            @NonCPS
            @Override
            public String toString() {return "session";}
        },
        dcScheduledJobs {
            @NonCPS
            @Override
            public String toString() {return "scheduledjobs";}
        },
        dcInfo {
            @NonCPS
            @Override
            public String toString() {
                return "info";
            }
        }
    }

    // enum SettEnum {
    //     seDbServer,
    //     seDbDatabase {
    //         @NonCPS
    //         @Override
    //         public String toString() {
    //             return "-db";
    //         }
    //     }       
    // }

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
        return params;
    }

    def setParam(Map<String, String> newParams, isIgnoreEmptyValues = true){
        def filtered;
        if (isIgnoreEmptyValues) {
            filtered = newParams.findAll { it.value != null }
        } else {
            filtered = newParams;
        }
        params << filtered;
        return params;
    }

    def pv(String key){
        params.get(key);
    }

    def setDb(String dbServer, String dbDatabase, String dbUser = null, String dbPwd = null) {
        setParam([(KEY_DB_DATABASE):dbDatabase, (KEY_DB_SERVER):dbServer, (KEY_DB_USER):dbUser, (KEY_DB_PWD):dbPwd]);
        connString = "/S${pv(KEY_DB_SERVER)}\\${pv(KEY_DB_DATABASE)}";
    }

    def setDbAuth(String dbUser, String dbPwd) {
        setParam([(KEY_DB_USER):dbUser, (KEY_DB_PWD):dbPwd]);
    }

    def setRepo(String repoPath, String repoUser = null, String repoPwd = null) {
        setParam([(KEY_REPO_PATH):repoPath, (KEY_REPO_USER):repoUser, (KEY_REPO_PWD):repoPwd]);
    }

    def setRepoAuth(String repoUser, String repoPwd) {
        setParam([(KEY_REPO_USER):repoUser, (KEY_REPO_PWD):repoPwd]);
    }

    def launchUserInterface(Boolean updateMetadata){
        String launchParam = 'ЗавершитьРаботуСистемы;';
        if (updateMetadata) {launchParam = launchParam.concat('ЗапуститьОбновлениеИнформационнойБазы;')}
        return execScript(pathToDeployka, DeplCommand.dcRun, connString, "-db-user", pv(KEY_DB_USER), "-db-pwd", pv(KEY_DB_PWD), "-command",
            launchParam, "-execute", pv(KEY_PATH_TO_SERVICE_EPF), "-uccode", ucCode);
    }

    def setRAS(String rasServer, String racUtilPath) {
        setParam([(KEY_RAS_SERVER):rasServer, (KEY_RAC_UTIL_PATH):racUtilPath]);
    }

    private def setLockStatus(DeplCommand command, Boolean locked){
        String op = locked ? "lock" : "unlock";
        String[] execParams = [pathToDeployka, command, op, "-ras", "${pv(KEY_RAS_SERVER)}", "-rac", "${pv(KEY_RAC_UTIL_PATH)}", 
            "-db", "${pv(KEY_DB_DATABASE)}", "-db-user", "${pv(KEY_DB_USER)}", "-db-pwd", "${pv(KEY_DB_PWD)}"];
        if (res==LockResEnum.lrUserSeanse) {
            execParams = execParams + ["-lockuccode", ucCode];
        }
        return execScript(execParams);
    }

    def setLockStatusForUsers(Boolean locked) {
        return setLockStatus(DeplCommand.dcSession, locked);
    }

    def setLockStatusForBackgrounds(Boolean locked) {
        return setLockStatus(DeplCommand.dcScheduledJobs, locked);
    }

    def kissSessions(String appFilter = null) {
        String[] execParams = [pathToDeployka, LockResEnum.lrUserSeanse, "kill", "-ras", pv(KEY_RAS_SERVER), "-rac", pv(KEY_RAC_UTIL_PATH), 
            "-db", pv(KEY_DB_DATABASE), "-db-user", pv(KEY_DB_USER), "-db-pwd", pv(KEY_DB_PWD), "-lockuccode", ucCode, "-with-nolock", "y"];
        if (appFilter!=null) {
            execParams = execParams + ["-filter", appFilter];
        }
        // echo execParams;
        return execScript(execParams);
    }

    def updateFromPackage(String pathToPackage) {
        String[] execParams = [pathToDeployka, DeplCommand.dcLoadCfg, connString, pathToPackage, "/mode", "-auto", 
            "-db-user", pv(KEY_DB_USER), "-db-pwd"];
        return execScript(execParams);
    }

 }