
package Energos.Jenkins.OScript;

// import Energos.Jenkins.OScript.LockResourcesEnum;

class DeploykaHelper extends OScriptHelper {

    String pathToDeployka;
    Map<Object, String> params = [:];
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
                    if (retVal.startsWith(':')){
                        retVal = retVal.substring(1);
                    }
                    break;
                }
            }
            scanner.close(); 
            return retVal;       
        }
    }

    enum ParamsEnum {
        peDbServer,
        peDbConnString,
        pePathToServiceEpf{
            @NonCPS
            @Override
            public String toString() {return "-execute";}
        },
        peDbDatabase{
            @NonCPS
            @Override
            public String toString() {return "-db";}
        },
        peDbUser{
            @NonCPS
            @Override
            public String toString() {return "-db-user";}
        },
        peDbPwd{
            @NonCPS
            @Override
            public String toString() {return "-db-pwd";}
        },
        peRepoPath,
        peRepoUser{
            @NonCPS
            @Override
            public String toString() {return "-storage-user";}
        },
        peRepoPwd{
            @NonCPS
            @Override
            public String toString() {return "-storage-pwd";}
        },
        peLaunchParam{
            @NonCPS
            @Override
            public String toString() {return "-command";}
        },
        peConfigUpdateMode{
            @NonCPS
            @Override
            public String toString() {return "/mode";}
        },
        peRASServer{
            @NonCPS
            @Override
            public String toString() {return "-ras";}
        },
        peRACUtility{
            @NonCPS
            @Override
            public String toString() {return "-rac";}
        }
    }

    class ExecParams<String> extends ArrayList<String>{

        private Object params;

        ExecParams(Object params){
            super();
            this.params = params;
        }

        // @NonCPS
        def init(String module) {
            clear();
            def retVal = addValue(module);
            // echo("returns from init: $retVal");
            return this;
        }

        // @NonCPS
        def addValue(def value) {
            // echo("value.class: ${value.class}\nvalue.class==ParamsEnum.class: ${value.class==ParamsEnum.class}")
            if (value.class==ParamsEnum.class) {
                addValue(params.get(value))    
            } else {
                String strVal = "${value}";
                add(strVal);
            };
            return this;
        }

        // @NonCPS
        def addCommand(DeplCommand command){
            return addValue(command);
        }

        // @NonCPS
        def addPair(ParamsEnum param) {
            return addValue(param.toString())
                    .addValue(params.get(param));
        }

        // @NonCPS
        def addPair(String parKey, String parVal) {
            return addValue(parKey).addValue(parVal);
        }
    } 

    public DeploykaHelper(def paramScript, String pathToDeployka, String pathToServiceEPF = null){
        
        super(paramScript); 

        this.pathToDeployka = pathToDeployka;
        setParam(ParamsEnum.pePathToServiceEpf, pathToServiceEPF, pathToServiceEPF!=null);
        configInfo = new ConfigInfo();
        
        execParamsList = new ExecParams(params);

    }

    @NonCPS
    def setParam(def paramKey, String paramValue, Boolean isApply = true){
        if (isApply) {
            // if (paramKey.class==ParamsEnum.class) {
            //     params.put((paramKey), paramValue);
            // } else {
            //     params.put(paramKey, paramValue);
            // }
            params.put(paramKey, paramValue);
        };
        return params;
    }

    @NonCPS
    def setParam(Map<Object, String> newParams, isIgnoreEmptyValues = true){
        def filtered;
        if (isIgnoreEmptyValues) {
            filtered = newParams.findAll { it.value != null }
        } else {
            filtered = newParams;
        }
        params << filtered;
        return params;
    }

    @NonCPS
    def pv(def key){
        params.get(key);
    }

    @NonCPS
    def setDb(String dbServer, String dbDatabase, String dbUser = null, String dbPwd = null) {
        setParam([(ParamsEnum.peDbDatabase): dbDatabase, (ParamsEnum.peDbServer):dbServer, (ParamsEnum.peDbUser):dbUser, (ParamsEnum.peDbPwd):dbPwd]);
        setParam((ParamsEnum.peDbConnString), "/S$dbServer\\$dbDatabase".toString());
    }

    @NonCPS
    def setDbAuth(String dbUser, String dbPwd) {
        setParam([(ParamsEnum.peDbUser):dbUser, (ParamsEnum.peDbPwd):dbPwd]);
    }

    @NonCPS
    def setRepo(String repoPath, String repoUser = null, String repoPwd = null) {
        setParam([(ParamsEnum.peRepoPath):repoPath, (ParamsEnum.peRepoUser):repoUser, (ParamsEnum.peRepoPwd):repoPwd]);
    }

    @NonCPS
    def setRepoAuth(String repoUser, String repoPwd) {
        setParam([(ParamsEnum.peRepoUser):repoUser, (ParamsEnum.peRepoPwd):repoPwd]);
    }

    // @NonCPS
    def launchUserInterface(Boolean updateMetadata){
        
        // echo "executing launchUserInterface"
        def retVal;

        String launchParam = 'ЗавершитьРаботуСистемы;';
        if (updateMetadata) {launchParam = launchParam.concat('ЗапуститьОбновлениеИнформационнойБазы;')}
        setParam(ParamsEnum.peLaunchParam, launchParam);

        // echo ("executing script");
        retVal = execScript(
                execParamsList.init(pathToDeployka)
                .addCommand(DeplCommand.dcRun)
                .addValue(ParamsEnum.peDbConnString)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
                .addPair(ParamsEnum.peLaunchParam)
                .addPair(ParamsEnum.pePathToServiceEpf)
                .addPair('-uccode', ucCode)
        );
        
        // echo ("retVal: $retVal\nreading log: ${resultLog}");
        configInfo.readFromLog(resultLog);
        // echo configInfo.version;

        // retVal = resultCode==0;
        retVal;
    }

    // @NonCPS
    // def launchUserInterfaceWith(Boolean updateMetadata, Closure closure){
    //     // echo "executing launchUserInterfaceWith"
    //     Boolean res = launchUserInterface(updateMetadata);
    //     closure(res);
    //     return res;
    // }

    def launchUserInterface(Boolean updateMetadata, Closure closure){
        // echo "executing launchUserInterfaceWith"
        Boolean res = launchUserInterface(updateMetadata);
        closure(res);
        return res;
    }

    @NonCPS
    def setRAS(String rasServer, String racUtilPath) {
        setParam([(ParamsEnum.peRASServer):rasServer, (ParamsEnum.peRACUtility):racUtilPath]);
    }

    // @NonCPS
    private def setLockStatus(DeplCommand command, Boolean locked){
        String op = locked ? "lock" : "unlock";
        def params = execParamsList.init(pathToDeployka)
                .addCommand(command)
                .addValue(op)
                .addPair(ParamsEnum.peRASServer)
                .addPair(ParamsEnum.peRACUtility)
                .addPair(ParamsEnum.peDbDatabase)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd);
        if (command==DeplCommand.dcSession) {
            params = params.addPair("-lockuccode", ucCode);
        };
        return execScript(params);
    }

    // @NonCPS
    def setLockStatusForUsers(Boolean locked) {
        return setLockStatus(DeplCommand.dcSession, locked);
    }

    def setLockStatusForUsersWith(Boolean locked, Closure closure) {
        def retVal = setLockStatusForUsers(locked);
        closure(retVal);
        return retVal;
    }

    // @NonCPS
    def setLockStatusForBackgrounds(Boolean locked) {
        return setLockStatus(DeplCommand.dcScheduledJobs, locked);
    }

    def setLockStatusForBackgroundsWith(Boolean locked, Closure closure) {
        def retVal = setLockStatusForBackgrounds(locked);
        closure(retVal);
        return retVal;
    }

    @NonCPS
    def kissSessions(String appFilter = null) {
        String[] execParams = [pathToDeployka, LockResEnum.lrUserSeanse, "kill", "-ras", pv(KEY_RAS_SERVER), "-rac", pv(KEY_RAC_UTIL_PATH), 
            "-db", pv(KEY_DB_DATABASE), "-db-user", pv(KEY_DB_USER), "-db-pwd", pv(KEY_DB_PWD), "-lockuccode", ucCode, "-with-nolock", "y"];
        if (appFilter!=null) {
            execParams = execParams + ["-filter", appFilter];
        }
        // echo execParams;
        return execScript(execParams);
    }

    // @NonCPS
    def updateFromPackage(String pathToPackage) {
        // String[] execParams = [pathToDeployka, DeplCommand.dcLoadCfg, connString, pathToPackage, "/mode", "-auto", 
        //     "-db-user", pv(KEY_DB_USER), "-db-pwd"];
        return execScript(
                execParamsList.init(pathToDeployka)
                .addCommand(DeplCommand.dcLoadCfg)
                .addValue(ParamsEnum.peDbConnString)
                .addValue(pathToPackage)
                .addPair(ParamsEnum.peConfigUpdateMode, "-auto")
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
        );
    }

    def updateFromPackage(String pathToPackage, Closure closure) {
        def retVal = updateFromPackage(pathToPackage);
        closure(retVal);
        retVal;
    }

 }