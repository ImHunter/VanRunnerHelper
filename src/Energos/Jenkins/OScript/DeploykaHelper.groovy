
package Energos.Jenkins.OScript;

// import Energos.Jenkins.OScript.LockResourcesEnum;

class DeploykaHelper extends OScriptHelper {

    String pathToDeployka;
    Map<String, String> params = [:];
    String ucCode = 'blocked';

    ConfigInfo configInfo;
    ExecParams execParamsList;

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
        },
        dcFileOperations {
            @NonCPS
            @Override
            public String toString() { return "fileop"; }
        }
    }

    class ConfigInfo {
        
        Boolean isChanged;
        String shortName;
        String version;

        def readFromLog(String log) {
            
            String paramValue;

            isChanged = null;
            paramValue = readParamValue(log, 'CONFIG_STATE');
            // echo "value of CONFIG_STATE: ${paramValue}";
            if (paramValue!=null) {
                if (paramValue.toUpperCase().equals('CONFIG_CHANGED')) {
                    isChanged = true
                } else {
                    if (paramValue.toUpperCase().equals('CONFIG_NOT_CHANGED')) {
                        isChanged = false
                    }    
                }
            }
            shortName = readParamValue(log, 'SHORT_CONFIG_NAME');
            version = readParamValue(log, 'CONFIG_VERSION');
        }
        private String readParamValue(String log, String paramName) {
            String retVal;
            Scanner scanner = new Scanner(log);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                Integer posParam = line.toUpperCase().indexOf(paramName.toUpperCase());
                if (posParam>=0) {
                    retVal = line.substring(posParam + paramName.length());
                    if (retVal.startsWith(':'))
                        retVal = retVal.substring(1);
                    break;
                }
            }
            scanner.close(); 
            return retVal;       
        }
    }

    enum ParamsEnum {

    }

    public class ExecParams<String> extends ArrayList<String>{

        @NonCPS
        def init() {
            clear();
            add(pathToDeployka);
        }

    } 

    public DeploykaHelper(def paramScript, String pathToDeployka, String pathToServiceEPF = null){
        
        super(paramScript); 

        this.pathToDeployka = pathToDeployka;
        setParam((KEY_PATH_TO_SERVICE_EPF), pathToServiceEPF, pathToServiceEPF!=null);
        configInfo = new ConfigInfo();
        
        execParamsList = new ExecParams();
        execParamsList.init();
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
        Boolean retVal;
        String launchParam = 'ЗавершитьРаботуСистемы;';
        if (updateMetadata) {launchParam = launchParam.concat('ЗапуститьОбновлениеИнформационнойБазы;')}
        retVal = execScript(pathToDeployka, DeplCommand.dcRun, connString, "-db-user", pv(KEY_DB_USER), "-db-pwd", pv(KEY_DB_PWD), "-command",
            launchParam, "-execute", pv(KEY_PATH_TO_SERVICE_EPF), "-uccode", ucCode);
        configInfo.readFromLog(resultLog);
        return retVal;
    }

    // @NonCPS
    def launchUserInterfaceWith(Boolean updateMetadata, Closure closure){
        Boolean res = launchUserInterface(updateMetadata);
        closure(res);
        return res;
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