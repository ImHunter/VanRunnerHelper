package Energos.Jenkins.OScript

import java.lang.*

/**
 * Класс-обертка для операций Деплойки.
 */
class DeploykaHelper extends OScriptHelper {

    /**
     * Тип оповещения - неопределено
     */
    final static int NOTIFY_TYPE_UNDEFINED = 0
    /**
     * Тип оповещения - перед выполнением операции
     */
    final static int NOTIFY_TYPE_BEFORE = 1
    /**
     * Тип оповещения - после выполнения операции
     */
    final static int NOTIFY_TYPE_AFTER = 2

    /**
     * Вид операции - неопределено.
     */
    final static int OP_UNDEFINED = 0
    /**
     * Вид операции - запуск в режиме 1С:Предприятик
     */
    final static int OP_LAUNCH_USER_INTERFACE = 1
    /**
     * Вид опреации - блокировка/разблокировка пользовательских сеансов
     */
    final static int OP_SET_LOCK_USERS = 2
    /**
     * Вид операции - блокировка/разблокировка регламентных заданий
     */
    final static int OP_SET_LOCK_BACKGROUNDS = 3
    /**
     * Вид операции - завершение сеансов
     */
    final static int OP_KILL_SESSIONS = 4
    /**
     * Вид операции - обновление конфигурации из пакета обновления
     */
    final static int OP_UPDATE_CONFIG_FROM_PACKAGE = 5
    /**
     * Вид операции - обновление конфигурации из хранилища
     */
    final static int OP_UPDATE_CONFIG_FROM_REPO = 6
    /**
     * Вид операции - отключение конфигурации от хранилища
     */
    final static int OP_UNBIND_REPO = 7
//    final static int OP_ =
//    final static int OP_ =

    /**
     * Путь к выполняемому скрипту Деплойка.
     * Скрипт может быть и любой другой
     */
    public String pathToDeployka
    /**
     * Свойства, которые могут быть использованы при выполнении скрипта.
     * Задаются методами set... (например, setDb(...)).
     */
    public Map<Object, String> params = [:]
    /**
     * Значение, которое может быть указано, например, при блокировке сеансов. Т.е., то, что передается в ключе /UC
     */
    public String ucCode = 'blocked'
    /**
     * Closure, которая может быть использована для логирования операций. Вызывается внутри метода notifyAbout().
     */
    public Closure notifyClosure = null
    /**
     * Объект, содержащий информацию о конфигурации.
     * Информация заполняется при выполнении запуска 1С в режиме Предприятие. При этом, происходит запуск специализированной
     * внешней обработки. Результаты ее работы (лог) разбираются и интерпретируются.
     */
    public ConfigInfo configInfo

    /**
     * Перечисление с возможными командами Деплойки
     */
    enum DeplCommand {
        /**
         * Запуск в режиме Предприятие
         */
        dcRun {
            @NonCPS
            @Override
            String toString() { return "run" }
        },
        /**
         * Обновление конфигурации из пакета обновлений
         */
        dcLoadCfg {
            @NonCPS
            @Override
            String toString() {return "loadcfg" }
        },
        /**
         * Обновление конфигурации из хранилища
         */
        dcLoadRepo {
            @NonCPS
            @Override
            String toString() {return "loadrepo" }
        },
        /**
         * Отключение конфигурации от хранилища
         */
        dcUnbindRepo {
            @NonCPS
            @Override
            String toString() {return "unbindrepo" }
        },
        /**
         * Операции с сеансами
         */
        dcSession {
            @NonCPS
            @Override
            String toString() {return "session" }
        },
        /**
         * Операции с регламентными заданиями
         */
        dcScheduledJobs {
            @NonCPS
            @Override
            String toString() {return "scheduledjobs" }
        },
        /**
         * Получение информации о базе данных
         */
        dcInfo {
            @NonCPS
            @Override
            String toString() {
                return "info"
            }
        },
        /**
         * Файловые операции
         */
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
     * В notifyClosure передаются несколько параметров: msgText - сообщаемое сообщение; текущий объект this, msgKind, msgType, params.
     * Такая детализация задумана для того, чтобы можно было достаточно просто обрабатывать выводимые оповещения. И при необходимости -
     * частично или полностью переписать алгоритм формирования текстовки и состава оповещений.
     * @param msgText Сообщаемое сообщение.
     * @param msgKind Вид операции, о которой происходит оповещение.
     * Используются значения констант OP_*
     * @param msgType Тип оповещения (до, перед, не задано).
     * Используются значения констант NOTIFY_TYPE_*
     * @param params Дополнительные параметры, которые могут быть переданы с оповещением.
     * Этим параметром передаются параметры выполнения какого-либо метода.
     * К примеру, при выполнении метода setLockStatusForUsers(locked), здесь передается параметр locked.
     */
    protected void notifyAbout(def msgText, def msgKind = OP_UNDEFINED, def msgType = NOTIFY_TYPE_UNDEFINED, Object... params){
        if (notifyClosure!=null)
            notifyClosure.call(msgText, this, msgKind, msgType, params)
    }

// @NonCPS
    @Override
    void selfTest() {
        // super.selfTest();
        def params
        isTestMode = true
        def cl = {msg ->
            echo("notify via closure. msg: $msg")
        }
        notifyClosure = cl
        notifyAbout('TEST CLOSURE MESSAGE')


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

        updateConfigFromPackage('path to package')

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

        def msg = 'Попытка ' + (locked ? 'установки': 'снятия') + ' блокировки сеансов пользователей'
        notifyAbout(msg, getOP_SET_LOCK_USERS(), getNOTIFY_TYPE_BEFORE())

        boolean retVal = setLockStatus(DeplCommand.dcSession, locked)

        msg = (locked ? 'Установка': 'Снятие') + ' блокировки сеансов пользователей ' +
                (retVal ? 'успешно' : 'не') + ' выполнена'
        notifyAbout(msg, getOP_SET_LOCK_USERS(), getNOTIFY_TYPE_AFTER())

        retVal
    }

    boolean setLockStatusForUsers(Boolean locked, Closure closure) {
        def retVal = setLockStatusForUsers(locked)
        closure(retVal)
        retVal
    }

    // @NonCPS
    boolean setLockStatusForBackgrounds(Boolean locked) {
        def msg = 'Попытка ' + (locked ? 'установки': 'снятия') + ' блокировки выполнения регламентных заданий'
        notifyAbout(msg, getOP_SET_LOCK_BACKGROUNDS(), getNOTIFY_TYPE_BEFORE(), locked)
        boolean retVal = setLockStatus(DeplCommand.dcScheduledJobs, locked)
        msg = (locked ? 'Установка': 'Снятие') + ' блокировки выполнения регламентных заданий ' +
                (retVal ? 'успешно' : 'не') + ' выполнена'
        notifyAbout(msg, getOP_SET_LOCK_BACKGROUNDS(), getNOTIFY_TYPE_AFTER(), locked)
        retVal
    }

    boolean setLockStatusForBackgrounds(Boolean locked, Closure closure) {
        def retVal = setLockStatusForBackgrounds(locked)
        closure(retVal)
        retVal
    }

    // @NonCPS
    def killSessions(Boolean withNoLock = true, String appFilter = '') {
        def msg = 'Попытка завершения сеансов' + (appFilter!=null && appFilter!='' ? '' : '; фильтр: ' + appFilter)
        notifyAbout(msg, getOP_KILL_SESSIONS(), getNOTIFY_TYPE_BEFORE())
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
        msg = 'Завершение сеансов ' + (retVal ? 'успешно' : 'не') + ' выполнено'
        notifyAbout(msg, getOP_KILL_SESSIONS(), getNOTIFY_TYPE_BEFORE())
        retVal
    }

    // @NonCPS
    boolean updateConfigFromPackage(String pathToPackage) {
        def msg = 'Попытка обновления конфигурации из пакета обновлений'
        notifyAbout(msg, getOP_UPDATE_CONFIG_FROM_PACKAGE(), getNOTIFY_TYPE_BEFORE(), pathToPackage)
        def retVal = execScript(
                new ExecParams(this)
                .addCommand(DeplCommand.dcLoadCfg)
                .addValue(ParamsEnum.peDbConnString)
                .addValue(pathToPackage)
                .addPair(ParamsEnum.peConfigUpdateMode, "-auto")
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
        )
        notifyAbout(msg, getOP_UPDATE_CONFIG_FROM_PACKAGE(), getNOTIFY_TYPE_AFTER(), pathToPackage)
        retVal
    }

    boolean updateConfigFromPackage(String pathToPackage, Closure closure) {
        def retVal = updateConfigFromPackage(pathToPackage)
        closure(retVal)
        retVal
    }

    def updateConfigFromRepo() {
        def msg = 'Попытка обновления конфигурации из хранилища'
        notifyAbout(msg, getOP_UPDATE_CONFIG_FROM_REPO(), getNOTIFY_TYPE_BEFORE())
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
        msg = 'Обновление конфигурации из хранилища ' + (retVal ? 'успешно' : 'не') + ' выполнено'
        notifyAbout(msg, getOP_UPDATE_CONFIG_FROM_REPO(), getNOTIFY_TYPE_AFTER())
        retVal
    }

    def unbindRepo() {
        def msg = 'Попытка отключения конфигурации от хранилища'
        notifyAbout(msg, getOP_UNBIND_REPO(), getNOTIFY_TYPE_BEFORE())
        def retVal = execScript(
                new ExecParams(this)
                .addCommand(DeplCommand.dcUnbindRepo)
                .addValue(ParamsEnum.peDbConnString)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
        )
        msg = 'Отключение конфигурации от хранилища ' + (retVal ? 'успешно' : 'не') + ' выполнено'
        notifyAbout(msg, getOP_UNBIND_REPO(), getNOTIFY_TYPE_AFTER())
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