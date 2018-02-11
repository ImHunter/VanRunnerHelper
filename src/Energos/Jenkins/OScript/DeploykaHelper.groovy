package Energos.Jenkins.OScript

import java.lang.*

/**
 * Класс-обертка для операций Деплойки.
 */
class DeploykaHelper extends OScriptHelper {

    final static int NOTIFY_TYPE_UNDEFINED = 0
    final static int NOTIFY_TYPE_BEFORE = 1
    final static int NOTIFY_TYPE_AFTER = 2

    final static int OP_UNDEFINED = 0
    final static int OP_LAUNCH_USER_INTERFACE = 1

    public String pathToDeployka
    public Map<Object, String> params = [:]
    public String ucCode = 'blocked'
    /**
     * Closure, которая может быть использована для логирования операций. Вызывается внутри метода notifyAbout
     */
    public Closure notifyClosure = null
    public ConfigInfo configInfo

    enum DeplCommand {
        dcRun {
            @NonCPS
            @Override
            String toString() { return "run" }
        },
        dcLoadCfg {
            @NonCPS
            @Override
            String toString() {return "loadcfg" }
        },
        dcLoadRepo {
            @NonCPS
            @Override
            String toString() {return "loadrepo" }
        },
        dcUnbindRepo {
            @NonCPS
            @Override
            String toString() {return "unbindrepo" }
        },
        dcSession {
            @NonCPS
            @Override
            String toString() {return "session" }
        },
        dcScheduledJobs {
            @NonCPS
            @Override
            String toString() {return "scheduledjobs" }
        },
        dcInfo {
            @NonCPS
            @Override
            String toString() {
                return "info"
            }
        },
        dcFileOperations {
            @NonCPS
            @Override
            String toString() { return "fileop" }
        }
    }

    class ConfigInfo {
        
        Boolean isChanged
        String shortName
        String version
        String platform

        private void readLogInfo(String log) {
            
            String paramValue

            isChanged = null
            paramValue = readParamValue(log, 'CONFIG_STATE')
            // echo "value of CONFIG_STATE: ${paramValue}";
            if (paramValue!=null) {
                if (paramValue.toUpperCase() == 'CONFIG_CHANGED') {
                    isChanged = true
                } else {
                    if (paramValue.toUpperCase() == 'CONFIG_NOT_CHANGED') {
                        isChanged = false
                    }    
                }
            }
            shortName = readParamValue(log, 'SHORT_CONFIG_NAME')
            version = readParamValue(log, 'CONFIG_VERSION')
            platform = readParamValue(log, 'PLATFORM')
        }

        private String readParamValue(String log, String paramName) {
            String retVal
            Scanner scanner = new Scanner(log)
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine()
                Integer posParam = line.toUpperCase().indexOf(paramName.toUpperCase())
                if (posParam>=0) {
                    retVal = line.substring(posParam + paramName.length())
                    if (retVal.startsWith(':')){
                        retVal = retVal.substring(1)
                    }
                    break
                }
            }
            scanner.close()
            return retVal
        }
    }

    enum ParamsEnum {
        peDbServer,
        peDbConnString,
        pePathToServiceEpf{
            @NonCPS
            @Override
            String toString() {return "-execute" }
        },
        peDbDatabase{
            @NonCPS
            @Override
            String toString() {return "-db" }
        },
        peDbUser{
            @NonCPS
            @Override
            String toString() {return "-db-user" }
        },
        peDbPwd{
            @NonCPS
            @Override
            String toString() {return "-db-pwd" }
        },
        peRepoPath,
        peRepoUser{
            @NonCPS
            @Override
            String toString() {return "-storage-user" }
        },
        peRepoPwd{
            @NonCPS
            @Override
            String toString() {return "-storage-pwd" }
        },
        peLaunchParam{
            @NonCPS
            @Override
            String toString() {return "-command" }
        },
        peConfigUpdateMode{
            @NonCPS
            @Override
            String toString() {return "/mode" }
        },
        peRASServer{
            @NonCPS
            @Override
            String toString() {return "-ras" }
        },
        peRACUtility{
            @NonCPS
            @Override
            String toString() {return "-rac" }
        },
        peFileOpDirectory{
            @NonCPS
            @Override
            String toString() {return "-dir" }
        }
    }

    class ExecParams<String> extends ArrayList<String>{

        def params

        ExecParams(def owner){
            super()
            this.params = ((DeploykaHelper) owner).params
            addValue(((DeploykaHelper) owner).pathToDeployka)
        }

        ExecParams(DeploykaHelper owner, DeplCommand command){
            super()
            this.params = ((DeploykaHelper) owner).params
            addValue(((DeploykaHelper) owner).pathToDeployka)
            if (command!=null) {
                addValue(command)
            }
        }

        @NonCPS
        ExecParams addValue(def value) {
            if (value==null) {
                add(qStr())
            } else {
                if (value.class==ParamsEnum.class) {
                    addValue(params.get(value))    
                } else {
                    String strVal = "${value}".toString()
                    if (strVal.contains(' '))
                        strVal = qStr(strVal)
                    add(strVal)
                }
            }
            this
        }

        // @NonCPS
        def addCommand(DeplCommand command){
            return addValue(command)
        }

        // @NonCPS
        def addPair(ParamsEnum param) {
            return addValue(param.toString())
                    .addValue(params.get(param))
        }

        // @NonCPS
        def addPair(String parKey, String parVal) {
            return addValue(parKey).addValue(parVal)
        }
    }

    DeploykaHelper(def paramScript, String pathToDeployka, String pathToServiceEPF = null){
        
        super(paramScript)

        this.pathToDeployka = qStr(pathToDeployka)

        setParam(ParamsEnum.pePathToServiceEpf, qStr(pathToServiceEPF), pathToServiceEPF!=null)
        configInfo = new ConfigInfo()

    }

    /**
     * Метод для оповещения о каком-либо событии.
     * Вызывает выполнение notifyClosure, если эта Closure задана.
     * В notifyClosure передаются несколько параметров: notifyMsg - сообщаемое сообщение; текущий объект this.
     * @param msg Сообщаемое сообщение.
     * @param withResetResult Сбрасывать ли значения полей resultCode и resultLog в null.
     * Значение параметра true используется для оповещений ПЕРЕД выполнением операции.
     */
    protected void notifyAbout(def msgText, def msgKind, def msgType, Object... params){
        if (notifyClosure!=null)
            notifyClosure.call(msgText, this, msgKind, msgType, params)
    }

// @NonCPS
    @Override
    void selfTest() {
        // super.selfTest();
        def params
        isTestMode = true

        setDb('server', 'db')
        testEcho("selfTest pathToDeployka: $pathToDeployka")

        setDbAuth('польззз', 'паророр')
        testEcho("executed setDbAuth('польззз', 'паророр')")

        params = new ExecParams(this)
        echo("test params new ExecParams(this): $params")

        params = new ExecParams(this)
        params.addValue(DeplCommand.dcRun)
        echo("test params new ExecParams(this) and params.addValue(DeplCommand.dcRun): $params")

        params = new ExecParams(this, DeplCommand.dcRun)
        echo("test params new ExecParams(this, DeplCommand.dcRun): $params")

        params = new ExecParams(this, DeplCommand.dcRun)
            .addPair(ParamsEnum.peDbServer)
            .addPair(ParamsEnum.peDbDatabase)
            .addPair(ParamsEnum.peDbUser)
            .addPair(ParamsEnum.peDbPwd)
            .addPair('custom key', 'custom value')

        echo("test params new ExecParams(this, DeplCommand.dcRun) and many params: $params")

        launchUserInterface()
        echo("executed launchUserInterface")

        setRAS('ras server', 'ras utility')
        echo("executed setRAS()")

        setLockStatus(DeplCommand.dcSession, true)
        echo("executed setLockStatus(DeplCommand.dcSession, true)")

        killSessions()
        echo("executed killSessions()")

        setRepo('repo path', 'repo-us', 'repo-pwd')
        echo("executed setRepo")

        updateConfigFromRepo()
        echo("executed updateConfigFromRepo")

        def cl = {msg ->
            echo("notify via closure. msg: $msg")
        }
        notifyClosure = cl
        notifyAbout('TEST CLOS')

        echo("finish of selfTest")
    }

    @NonCPS
    def setParam(def paramKey, String paramValue, Boolean isApply = true){
        if (isApply) {
            this.params.put paramKey, paramValue
        }
        this
    }

    // @NonCPS
    def setParam(Map<Object, String> newParams, isIgnoreEmptyValues = true){
        def filtered
        if (isIgnoreEmptyValues) {
            filtered = newParams.findAll { it.value != null }
        } else {
            filtered = newParams
        }
        params << filtered
        this
    }

    // @NonCPS
    void setDb(String dbServer, String dbDatabase, String dbUser = null, String dbPwd = null) {
        setParam([(ParamsEnum.peDbDatabase): dbDatabase, (ParamsEnum.peDbServer):dbServer, (ParamsEnum.peDbUser):dbUser, (ParamsEnum.peDbPwd):qStr(dbPwd)])
        setParam((ParamsEnum.peDbConnString), "/S$dbServer\\$dbDatabase".toString())
        this
    }

    @NonCPS
    void setDbAuth(String dbUser, String dbPwd) {
        setParam([(ParamsEnum.peDbUser):dbUser, (ParamsEnum.peDbPwd):qStr(dbPwd)])
        this
    }

    // @NonCPS
    void setRepo(String repoPath, String repoUser = null, String repoPwd = null) {
        setParam([(ParamsEnum.peRepoPath):repoPath, (ParamsEnum.peRepoUser):repoUser, (ParamsEnum.peRepoPwd):qStr(repoPwd)])
        this
    }

    @NonCPS
    void setRepoAuth(String repoUser, String repoPwd) {
        setParam([(ParamsEnum.peRepoUser):repoUser, (ParamsEnum.peRepoPwd):qStr(repoPwd)])
        this
    }

    // @NonCPS
    void setRAS(String rasServer, String racUtilPath) {
        setParam([(ParamsEnum.peRASServer):rasServer, (ParamsEnum.peRACUtility):racUtilPath])
        this
    }

    // @NonCPS
    boolean launchUserInterface(boolean updateMetadata = false){
       
        def retVal
        def opName = 'Запуск 1С:Предприятие'.concat( updateMetadata ? ' (с обновлением метаданных)' : '')

        String launchParam = 'ЗавершитьРаботуСистемы;'
        if (updateMetadata) 
            launchParam = launchParam.concat('ЗапуститьОбновлениеИнформационнойБазы;')
        setParam(ParamsEnum.peLaunchParam, qStr(launchParam))
        testEcho('подготовили параметры запуска launchParam')

        // echo ("executing script");
        notifyAbout(opName, getOP_LAUNCH_USER_INTERFACE(), getNOTIFY_TYPE_BEFORE(), updateMetadata)
        retVal = execScript(
                new ExecParams(this, DeplCommand.dcRun)
                .addValue(ParamsEnum.peDbConnString)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
                .addPair(ParamsEnum.peLaunchParam)
                .addPair(ParamsEnum.pePathToServiceEpf)
                .addPair('-uccode', ucCode)
        )
        configInfo.readLogInfo(resultLog)
        notifyAbout(opName, getOP_LAUNCH_USER_INTERFACE(), getNOTIFY_TYPE_AFTER(), updateMetadata)
        retVal
    }

    boolean launchUserInterface(Boolean updateMetadata, Closure closure){
        // echo "executing launchUserInterfaceWith"
        boolean res = launchUserInterface(updateMetadata)
        closure(res)
        res
    }

    // @NonCPS
    private boolean setLockStatus(DeplCommand command, Boolean locked){
        String op = locked ? "lock" : "unlock"
        def params = new ExecParams(this, command)
                .addValue(op)
                .addPair(ParamsEnum.peRASServer)
                .addPair(ParamsEnum.peRACUtility)
                .addPair(ParamsEnum.peDbDatabase)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
        if (command==DeplCommand.dcSession) {
            params = params.addPair("-lockuccode", ucCode)
        }
        execScript(params)
    }

    // @NonCPS
    boolean setLockStatusForUsers(Boolean locked) {
        notifyAbout('Попытка ' + (locked ? 'установки': 'снятия') + ' блокировки сеансов пользователей', true)
        boolean retVal = setLockStatus(DeplCommand.dcSession, locked)
        notifyAbout((locked ? 'Установка': 'Снятие') + ' блокировки сеансов пользователей ' +
                (retVal ? 'успешно' : 'не') + ' выполнена')
        retVal
    }

    boolean setLockStatusForUsers(Boolean locked, Closure closure) {
        def retVal = setLockStatusForUsers(locked)
        closure(retVal)
        retVal
    }

    // @NonCPS
    boolean setLockStatusForBackgrounds(Boolean locked) {
        notifyAbout('Попытка ' + (locked ? 'установки': 'снятия') + ' блокировки выполнения регламентных заданий', true)
        boolean retVal = setLockStatus(DeplCommand.dcScheduledJobs, locked)
        notifyAbout((locked ? 'Установка': 'Снятие') + ' блокировки выполнения регламентных заданий ' +
                (retVal ? 'успешно' : 'не') + ' выполнена')
        retVal
    }

    boolean setLockStatusForBackgrounds(Boolean locked, Closure closure) {
        def retVal = setLockStatusForBackgrounds(locked)
        closure(retVal)
        retVal
    }

    // @NonCPS
    def killSessions(Boolean withNoLock = true, String appFilter = null) {
        notifyAbout('Попытка завершения сеансов' + (appFilter!=null && appFilter!='' ? '' : '; фильтр: ' + appFilter), true)
        def params = new ExecParams(this, DeplCommand.dcSession)
                .addValue('kill')
                .addPair(ParamsEnum.peRASServer)
                .addPair(ParamsEnum.peRACUtility)
                .addPair(ParamsEnum.peDbDatabase)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
                .addPair("-lockuccode", ucCode)
        if (withNoLock) {
            params = params.addPair("-with-nolock", "y")
        }
        if (appFilter!=null && appFilter!='') {
            params = params.addPair("-filter", appFilter)
        }
        // echo execParams;
        def retVal = execScript(params)
        notifyAbout('Завершение сеансов ' + (retVal ? 'успешно' : 'не') + ' выполнено')
        retVal
    }

    // @NonCPS
    boolean updateConfigFromPackage(String pathToPackage) {
        notifyAbout('Попытка обновления конфигурации из пакета обновлений', true)
        def retVal = execScript(
                new ExecParams(this)
                .addCommand(DeplCommand.dcLoadCfg)
                .addValue(ParamsEnum.peDbConnString)
                .addValue(pathToPackage)
                .addPair(ParamsEnum.peConfigUpdateMode, "-auto")
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
        )
        notifyAbout('Обновление конфигурации из пакета обновлений ' + (retVal ? 'успешно' : 'не') + ' выполнено')
        retVal
    }

    boolean updateConfigFromPackage(String pathToPackage, Closure closure) {
        def retVal = updateConfigFromPackage(pathToPackage)
        closure(retVal)
        retVal
    }

    def updateConfigFromRepo() {
        notifyAbout('Попытка обновления конфигурации из хранилища', true)
        def retVal = execScript(
                new ExecParams(this)
                .addCommand(DeplCommand.dcLoadRepo)
                .addValue(ParamsEnum.peRepoPath)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
                .addPair(ParamsEnum.peRepoUser)
                .addPair(ParamsEnum.peRepoPwd)
                .addPair('-uccode', ucCode)
        )
        notifyAbout('Обновление конфигурации из хранилища ' + (retVal ? 'успешно' : 'не') + ' выполнено')
        retVal
    }

    def unbindRepo() {
        notifyAbout('Попытка отключения конфигурации от хранилища', true)
        def retVal = execScript(
                new ExecParams(this)
                .addCommand(DeplCommand.dcUnbindRepo)
                .addValue(ParamsEnum.peDbConnString)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
        )
        notifyAbout('Отключение конфигурации от хранилища ' + (retVal ? 'успешно' : 'не') + ' выполнено')
        retVal
    }

    def checkDirExists(String dir){
        def params = new ExecParams(this)
            .addCommand(DeplCommand.dcFileOperations)
            .addValue('direxists')
            .addPair(ParamsEnum.peFileOpDirectory, dir)
        execScript(params)
    }

    def checkDirExists(String dir, Closure closure){
        def retVal = checkDirExists(dir)
        closure(retVal)
        return retVal
    }

    // minModifyDT - минимальное время создания/изменения файлов в формате yyyyMMddHHmmss
    def findFiles(String dir, String fileMask, String minModifyDT = null) {
        def params = new ExecParams(this)
            .addCommand(DeplCommand.dcFileOperations)
            .addValue('fileexists')
            .addPair(ParamsEnum.peFileOpDirectory, dir)
            .addPair('-filename', fileMask)
        if (minModifyDT!=null && minModifyDT!='')
            params = params.addPair('-modified-dt', minModifyDT)
        return execScript(params)
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
            params = params.addPair("-filter", appFilter)
        }
        def retVal = execScript(params)
        if (closure!=null) {
            closure(retVal)
        }
        return retVal
    }   

 }