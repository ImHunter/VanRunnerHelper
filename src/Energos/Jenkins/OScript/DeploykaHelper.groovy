
package Energos.Jenkins.OScript;

class DeploykaHelper extends OScriptHelper {

    String pathToDeployka;
    Map<Object, String> params = [:];
    String ucCode = 'blocked';

    ConfigInfo configInfo;
    ExecParams execParamsList;

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
        dcLoadRepo {
            @NonCPS
            @Override
            public String toString() {return "loadrepo";}
        },
        dcUnbindRepo {
            @NonCPS
            @Override
            public String toString() {return "unbindrepo";}
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
        String platform;

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
            platform = readParamValue(log, 'PLATFORM');
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
            // @NonCPS
            @Override
            public String toString() {return "-execute";}
        },
        peDbDatabase{
            // @NonCPS
            @Override
            public String toString() {return "-db";}
        },
        peDbUser{
            // @NonCPS
            @Override
            public String toString() {return "-db-user";}
        },
        peDbPwd{
            // @NonCPS
            @Override
            public String toString() {return "-db-pwd";}
        },
        peRepoPath,
        peRepoUser{
            // @NonCPS
            @Override
            public String toString() {return "-storage-user";}
        },
        peRepoPwd{
            // @NonCPS
            @Override
            public String toString() {return "-storage-pwd";}
        },
        peLaunchParam{
            // @NonCPS
            @Override
            public String toString() {return "-command";}
        },
        peConfigUpdateMode{
            // @NonCPS
            @Override
            public String toString() {return "/mode";}
        },
        peRASServer{
            // @NonCPS
            @Override
            public String toString() {return "-ras";}
        },
        peRACUtility{
            // @NonCPS
            @Override
            public String toString() {return "-rac";}
        },
        peFileOpDirectory{
            // @NonCPS
            @Override
            public String toString() {return "-dir";}
        }
    }

    class ExecParams<String> extends ArrayList<String>{

        Map<Object, String> params = [:];

        ExecParams(def owner){
            super();
            // owner.testEcho("$owner".toString());

            this.params << (DeploykaHelper) owner.params;
            // testEcho(this.params);
            // addValue("${(DeploykaHelper) owner.pathToDeployka}");
         }

        ExecParams(DeploykaHelper owner, DeplCommand command){
            super();
            this.params = owner.params;
            addValue("${owner.pathToDeployka}");
            if (command!=null) {
                addCommand(command);
            }
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
            if (value==null) {
                add(qStr())
            } else {
                if (value.class==ParamsEnum.class) {
                    addValue(params.get(value))    
                } else {
                    String strVal = "${value}";
                    if (strVal.contains(' '))
                        strVal = qStr(strVal);
                    add(strVal);
                };
            }
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

        this.pathToDeployka = qStr(pathToDeployka);

        setParam(ParamsEnum.pePathToServiceEpf, qStr(pathToServiceEPF), pathToServiceEPF!=null);
        configInfo = new ConfigInfo();
        
        // execParamsList = new ExecParams(params);

    }

    // @NonCPS
    @Override
    public void selfTest() {
        // super.selfTest();
        def params;

        isTestMode = true;
        setDb('server', 'db');
        testEcho("selfTest pathToDeployka: $pathToDeployka");
        
        params = new ExecParams(this);
        echo("test params new ExecParams(this): $params");

            // .addPair(ParamsEnum.peDbServer)
            // .addPair(ParamsEnum.peDbDatabase)
            // .addPair(ParamsEnum.peDbUser)
            // .addPair(ParamsEnum.peDbPwd)
            // .addPair('custom key', 'custom value')
            // ;

        params = new ExecParams(this, DeplCommand.dcRun);
        echo("test params new ExecParams(this, DeplCommand.dcRun): $params")


        // launchUserInterface();
        echo("finish of selfTest");
    }

    @NonCPS
    def setParam(def paramKey, String paramValue, Boolean isApply = true){
        if (isApply) {
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
        setParam([(ParamsEnum.peDbDatabase): dbDatabase, (ParamsEnum.peDbServer):dbServer, (ParamsEnum.peDbUser):dbUser, (ParamsEnum.peDbPwd):qStr(dbPwd)]);
        setParam((ParamsEnum.peDbConnString), "/S$dbServer\\$dbDatabase".toString());
    }

    @NonCPS
    def setDbAuth(String dbUser, String dbPwd) {
        setParam([(ParamsEnum.peDbUser):dbUser, (ParamsEnum.peDbPwd):qStr(dbPwd)]);
    }

    @NonCPS
    def setRepo(String repoPath, String repoUser = null, String repoPwd = null) {
        setParam([(ParamsEnum.peRepoPath):repoPath, (ParamsEnum.peRepoUser):repoUser, (ParamsEnum.peRepoPwd):qStr(repoPwd)]);
    }

    @NonCPS
    def setRepoAuth(String repoUser, String repoPwd) {
        setParam([(ParamsEnum.peRepoUser):repoUser, (ParamsEnum.peRepoPwd):qStr(repoPwd)]);
    }

    // @NonCPS
    def launchUserInterface(Boolean updateMetadata = false){
       
        def retVal;

        String launchParam = 'ЗавершитьРаботуСистемы;';
        if (updateMetadata) 
            launchParam = launchParam.concat('ЗапуститьОбновлениеИнформационнойБазы;');
        setParam(ParamsEnum.peLaunchParam, qStr(launchParam));
        testEcho('подготовили параметры запуска launchParam');

        // echo ("executing script");
        retVal = execScript(
                // new ExecParams(this, DeplCommand.dcRun)
                new ExecParams(this)
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
        def params = new ExecParams(this)
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

    def setLockStatusForUsers(Boolean locked, Closure closure) {
        def retVal = setLockStatusForUsers(locked);
        closure(retVal);
        return retVal;
    }

    // @NonCPS
    def setLockStatusForBackgrounds(Boolean locked) {
        return setLockStatus(DeplCommand.dcScheduledJobs, locked);
    }

    def setLockStatusForBackgrounds(Boolean locked, Closure closure) {
        def retVal = setLockStatusForBackgrounds(locked);
        closure(retVal);
        return retVal;
    }

    // @NonCPS
    def kissSessions(Boolean withNoLock = true, String appFilter = null) {
        def params = new ExecParams(this)
                .addCommand(DeplCommand.dcSession)
                .addValue('kill')
                .addPair(ParamsEnum.peRASServer)
                .addPair(ParamsEnum.peRACUtility)
                .addPair(ParamsEnum.peDbDatabase)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
                .addPair("-lockuccode", ucCode);
        if (withNoLock==true) {
            params = params.addPair("-with-nolock", "y");
        }
        if (appFilter!=null && appFilter!='') {
            params = params.addPair("-filter", appFilter);
        }
        // echo execParams;
        return execScript(params);
    }

    // @NonCPS
    def updateConfigFromPackage(String pathToPackage) {
        return execScript(
                new ExecParams(this)
                .addCommand(DeplCommand.dcLoadCfg)
                .addValue(ParamsEnum.peDbConnString)
                .addValue(pathToPackage)
                .addPair(ParamsEnum.peConfigUpdateMode, "-auto")
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
        );
    }

    def updateConfigFromRepo() {
        return execScript(
                new ExecParams(this)
                .addCommand(DeplCommand.dcLoadRepo)
                .addValue(ParamsEnum.peRepoPath)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
                .addPair(ParamsEnum.peRepoUser)
                .addPair(ParamsEnum.peRepoPwd)
                .addPair('-uccode', ucCode)
        );
    }

    def unbindRepo() {
        return execScript(
                new ExecParams(this)
                .addCommand(DeplCommand.dcUnbindRepo)
                .addValue(ParamsEnum.peDbConnString)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
        );
    }

    def updateFromPackage(String pathToPackage, Closure closure) {
        def retVal = updateFromPackage(pathToPackage);
        closure(retVal);
        return retVal;
    }

    def checkDirExists(String dir){
        def params = new ExecParams(this)
            .addCommand(DeplCommand.dcFileOperations)
            .addValue('direxists')
            .addPair(ParamsEnum.peFileOpDirectory, dir);
        return execScript(params)==0;
    }

    def checkDirExists(String dir, Closure closure){
        def retVal = checkDirExists(dir);
        closure(retVal);
        return retVal;
    }

    // minModifyDT - минимальное время создания/изменения файлов в формате yyyyMMddHHmmss
    def findFiles(String dir, String fileMask, String minModifyDT = null) {
        def params = new ExecParams(this)
            .addCommand(DeplCommand.dcFileOperations)
            .addValue('fileexists')
            .addPair(ParamsEnum.peFileOpDirectory, dir)
            .addPair('-filename', fileMask);
        if (minModifyDT!=null && minModifyDT!='')
            params = params.addPair('-modified-dt', minModifyDT);
        return execScript(params);        
    } 

    // возврат Истина, если сеансы найдены
    def findSessions(String appFilter = null, Closure closure = null) {
        def params = new ExecParams(this)
                .addCommand(DeplCommand.dcSession)
                .addValue('search')
                .addPair(ParamsEnum.peRASServer)
                .addPair(ParamsEnum.peRACUtility)
                .addPair(ParamsEnum.peDbDatabase)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
        if (appFilter!=null && appFilter!='') {
            params = params.addPair("-filter", appFilter);
        }
        def retVal = execScript(params);
        if (closure!=null) {
            closure(retVal);
        }
        return retVal;
    }   

 }