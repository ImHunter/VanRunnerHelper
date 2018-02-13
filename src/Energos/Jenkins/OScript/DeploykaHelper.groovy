package Energos.Jenkins.OScript

import java.lang.*

/**
 * Класс-обертка для операций Деплойки.
 */
class DeploykaHelper extends OScriptHelper {

    //region Константы типов оповещений
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
    //endregion

    //region Константы видов оповещений (операций)
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
    /**
     * Вид операции - обновление БД
     */
    final static int OP_UPDATE_DB = 8
//    final static int OP_ =
    //endregion

    //region Поля public
    /**
     * Путь к выполняемому скрипту Деплойка.
     * Скрипт может быть и любой другой
     */
    public pathToDeployka
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
    public Closure notifyEvent = null
    /**
     * Объект, содержащий информацию о конфигурации.
     * Информация заполняется при выполнении запуска 1С в режиме Предприятие. При этом, происходит запуск специализированной
     * внешней обработки. Результаты ее работы (лог) разбираются и интерпретируются.
     */
    public ConfigInfo configInfo
    //endregion

    // region Перечисления
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
        },
        /**
         * Обновление БД
         */
        dcUpdateDB {
            @NonCPS
            @Override
            String toString() { return "dbupdate" }
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
        },
        peSessionFilter{
            @NonCPS
            @Override
            String toString() {return "-filter" }
        }
    }

    /**
     * Возможные приложения 1С
     * 1CV8 1CV8C WebClient Designer COMConnection WSConnection BackgroundJob WebServerExtension
     */
    enum AppNames{
        /**
         * Толстый клиент
         */
        appClient {
            @NonCPS
            @Override
            String toString() { return '1CV8' }
        },
        /**
         * Тонкий клиент
         */
        appClientThin {
            @NonCPS
            @Override
            String toString() {return '1CV8C' }
        },
        /**
         * Веб-клиент
         */
        appWebClient {
            @NonCPS
            @Override
            String toString() {return 'WebClient' }
        },
        /**
         * Конфигуратор
         */
        appDesigner {
            @NonCPS
            @Override
            String toString() {return 'Designer' }
        },
        /**
         * COM-коннектор
         */
        appComConnector {
            @NonCPS
            @Override
            String toString() {return 'COMConnection' }
        },
        /**
         * Вебсервис
         */
        appWS {
            @NonCPS
            @Override
            String toString() {return 'WSConnection' }
        },
        /**
         * Фоновое задание
         */
        appBackgroung {
            @NonCPS
            @Override
            String toString() {return 'BackgroundJob' }
        },
        /**
         * Вебсервисное расширение
         */
        appWebExt {
            @NonCPS
            @Override
            String toString() {return 'WebServerExtension' }
        }
    }

    // endregion

    // region Вложенные классы

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
            String retVal = null
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
            retVal
        }
    }

    class ExecParams<String> extends ArrayList<String>{

        def params

        ExecParams(DeploykaHelper owner){
            super()
            this.params = owner.params
            addValue(owner.pathToDeployka)
        }

        ExecParams(DeploykaHelper owner, DeplCommand command){
            super()
            this.params = owner.params
            addValue(owner.pathToDeployka)
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
                    java.lang.String strVal = "${value}".toString()
                    if (strVal.contains(' '))
                        strVal = qStr(strVal)
                    add(strVal)
                }
            }
            this
        }

        // @NonCPS
        ExecParams addCommand(DeplCommand command){
            addValue(command)
        }

        // @NonCPS
        ExecParams addPair(ParamsEnum param) {
            return addValue(param.toString())
                    .addValue(params.get(param))
        }

        // @NonCPS
        def addPair(String parKey, String parVal) {
            return addValue(parKey).addValue(parVal)
        }
    }

    class SessionFilter {

        private ArrayList<String> apps = new ArrayList<>()
        private ArrayList<String> names = new ArrayList<>()

        def setAppFilter(Object... apps) {
            apps.each { this.apps.add(it.toString()) }
            this
        }

        def setNamesFilter(Object... names) {
            names.each { this.names.add(it.toString()) }
            this
        }

        private String joinArray(def array) {
            String retVal
            retVal = array.join(';')
            retVal
        }

        @NonCPS
        @Override
        java.lang.String toString() {
            java.lang.String retVal = ''
            echo("SessionFilter enter apps.size()=$apps.size() names.size()=$names.size()")
            if (apps.size()!=0) {
                if (!retVal.equals('')) {
                    retVal = retVal.concat('|')
                }
                echo("SessionFilter test mess1 $retVal")
                retVal = retVal.concat('appid=').concat(joinArray(apps))
                echo("SessionFilter test mess2 $retVal")
            }
            if (names.size()!=0) {
                if (!retVal.equals('')) {
                    retVal = retVal.concat('|')
                }
                echo("SessionFilter test mess3 $retVal")
                retVal = retVal.concat('name=').concat(joinArray(names))
                echo("SessionFilter test mess4 $retVal")
            }
            echo("SessionFilter test mess5 $retVal")
            retVal
        }
    }

    // endregion

    DeploykaHelper(def paramScript, String pathToDeployka, String pathToServiceEPF = null){
        
        super(paramScript)

        this.pathToDeployka = qStr(pathToDeployka)

        setParam(ParamsEnum.pePathToServiceEpf, qStr(pathToServiceEPF), pathToServiceEPF!=null)
        configInfo = new ConfigInfo()

    }

    /**
     * Метод для оповещения о каком-либо событии.
     * Вызывает выполнение notifyEvent, если эта Closure задана.
     * В notifyEvent передаются несколько параметров: msgText - сообщаемое сообщение; текущий объект this, msgKind, msgType, params.
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
    protected void notifyAbout(String msgText, int msgKind = OP_UNDEFINED, int msgType = NOTIFY_TYPE_UNDEFINED, Object... params){
        if (notifyEvent!=null)
            notifyEvent.call(msgText, this, msgKind, msgType, params)
    }

// @NonCPS
    /**
     * Метод для самотестирования библиотеки.
     * Это некая замена модульному тестированию.
     * Устанавливается флаг тестирования (isTestMode = true), выполняются некие операции, флаг сбрасывается.
     */
    @Override
    void selfTest() {
        // super.selfTest();
        def params
        isTestMode = true
        def cl = {msg ->
            echo("notify via closure. msg: $msg")
        }
        notifyEvent = cl
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

//        killSessions(true, newSessionFilter().setAppFilter(AppNames.appClient, AppNames.appClientThin))
//        echo("executed killSessions()")

        setRepo('repo path', 'repo-us', 'repo-pwd')
        echo("executed setRepo")

        setDbAuth('newuser', 'newpwd')

        updateConfigFromRepo()
        echo("executed updateConfigFromRepo")

        updateDB()
        echo("DB updated")

        updateConfigFromPackage('path to package')

        def flt = newSessionFilter()
            .setAppFilter(AppNames.appClient, AppNames.appBackgroung, 'some app', 'other app')
            .setNamesFilter('админ', "польз")
            .toString()
        echo("Test filter filled: $flt")

        flt = newSessionFilter()
                .toString()
        echo("Test filter empty: $flt")

        echo("finish of selfTest")
        isTestMode = false
    }

    //region Установка параметров
    @NonCPS
    def setParam(def paramKey, String paramValue, Boolean isApply = true){
        if (isApply) {
            this.params.put paramKey, paramValue
        }
        this
    }

    // @NonCPS
    DeploykaHelper setParam(Map<Object, String> newParams, isIgnoreEmptyValues = true){
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
    DeploykaHelper setDb(String dbServer, String dbDatabase, String dbUser = null, String dbPwd = null) {
        setParam([(ParamsEnum.peDbDatabase): dbDatabase,
                  (ParamsEnum.peDbServer):dbServer,
                  (ParamsEnum.peDbUser):dbUser,
                  (ParamsEnum.peDbPwd):qStr(dbPwd)])
        setParam((ParamsEnum.peDbConnString), "/S$dbServer\\$dbDatabase".toString())
        this
    }

//    @NonCPS
    DeploykaHelper setDbAuth(String dbUser, String dbPwd) {
        setParam([(ParamsEnum.peDbUser):dbUser, (ParamsEnum.peDbPwd):qStr(dbPwd)])
        this
    }

    // @NonCPS
    DeploykaHelper setRepo(String repoPath, String repoUser = null, String repoPwd = null) {
        setParam([(ParamsEnum.peRepoPath):repoPath, (ParamsEnum.peRepoUser):repoUser, (ParamsEnum.peRepoPwd):qStr(repoPwd)])
        this
    }

    @NonCPS
    DeploykaHelper setRepoAuth(String repoUser, String repoPwd) {
        setParam([(ParamsEnum.peRepoUser):repoUser, (ParamsEnum.peRepoPwd):qStr(repoPwd)])
        this
    }

    // @NonCPS
    DeploykaHelper setRAS(String rasServer, String racUtilPath) {
        setParam([(ParamsEnum.peRASServer):rasServer, (ParamsEnum.peRACUtility):racUtilPath])
        this
    }

    // endregion

    SessionFilter newSessionFilter(){
        new SessionFilter()
    }

    // @NonCPS
    boolean launchUserInterface(boolean updateMetadata = false){
       
        def retVal
        def opName = 'Запуск 1С:Предприятие'.concat( updateMetadata ? ' (с обновлением метаданных)' : '')

        String launchParam = 'ЗавершитьРаботуСистемы;'
        if (updateMetadata) 
            launchParam = launchParam.concat('ЗапуститьОбновлениеИнформационнойБазы;')
        setParam( ParamsEnum.peLaunchParam, qStr(launchParam))
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
        ExecParams params = new ExecParams(this, command)
                .addValue(op)
                .addPair(ParamsEnum.peRASServer)
                .addPair(ParamsEnum.peRACUtility)
                .addPair(ParamsEnum.peDbDatabase)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
        if (command==DeplCommand.dcSession) {
            params = params.addPair('-lockuccode', ucCode)
        }
        execScript(params)
    }

    // @NonCPS
    boolean setLockStatusForUsers(Boolean locked) {

        String msg = 'Попытка ' + (locked ? 'установки': 'снятия') + ' блокировки сеансов пользователей'
        notifyAbout(msg, getOP_SET_LOCK_USERS(), getNOTIFY_TYPE_BEFORE())

        boolean retVal = setLockStatus(DeplCommand.dcSession, locked)

        msg = (locked ? 'Установка': 'Снятие') + ' блокировки сеансов пользователей ' +
                (retVal ? 'успешно' : 'не') + ' выполнена'
        notifyAbout(msg, getOP_SET_LOCK_USERS(), getNOTIFY_TYPE_AFTER())

        retVal
    }

    boolean setLockStatusForUsers(Boolean locked, Closure closure) {
        boolean retVal = setLockStatusForUsers(locked)
        closure(retVal)
        retVal
    }

    // @NonCPS
    boolean setLockStatusForBackgrounds(Boolean locked) {
        String msg = 'Попытка ' + (locked ? 'установки': 'снятия') + ' блокировки выполнения регламентных заданий'
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
    def killSessions(Boolean withNoLock = true, def appFilter = '') {
        String filter = appFilter.toString()
        String msg = 'Попытка завершения сеансов' + (appFilter!=null && !filter.isEmpty() ? '' : '; фильтр: ' + filter)
        notifyAbout(msg, getOP_KILL_SESSIONS(), getNOTIFY_TYPE_BEFORE())
        ExecParams params = new ExecParams(this, DeplCommand.dcSession)
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
        if (appFilter!=null && !filter.isEmpty()) {
            params = params.addPair(ParamsEnum.peSessionFilter, filter)
        }
        // echo execParams;
        boolean retVal = execScript(params)
        msg = 'Завершение сеансов '.concat(retVal ? 'успешно' : 'не').concat(' выполнено')
        notifyAbout(msg, getOP_KILL_SESSIONS(), getNOTIFY_TYPE_BEFORE())
        retVal
    }

    // @NonCPS
    boolean updateConfigFromPackage(String pathToPackage) {
        String msg = 'Попытка обновления конфигурации из пакета обновлений'
        notifyAbout(msg, getOP_UPDATE_CONFIG_FROM_PACKAGE(), getNOTIFY_TYPE_BEFORE(), pathToPackage)
        boolean retVal = execScript(
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
        String msg = 'Попытка обновления конфигурации из хранилища'
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
        ExecParams params = new ExecParams(this)
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
        ExecParams params = new ExecParams(this)
                .addCommand(DeplCommand.dcSession)
                .addValue('search')
                .addPair(ParamsEnum.peRASServer)
                .addPair(ParamsEnum.peRACUtility)
                .addPair(ParamsEnum.peDbDatabase)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
        if (appFilter!=null && !appFilter.toString().isEmpty()) {
            params = params.addPair(ParamsEnum.peSessionFilter, appFilter)
        }
        def retVal = execScript(params)
        if (closure!=null) {
            closure(retVal)
        }
        return retVal
    }

    boolean updateDB(Closure closure = null) {
        boolean retVal
        notifyAbout('Попытка обновления базы данных', OP_UPDATE_DB, NOTIFY_TYPE_BEFORE)
        ExecParams params = new ExecParams(this, DeplCommand.dcUpdateDB)
            .addValue(ParamsEnum.peDbConnString)
            .addPair(ParamsEnum.peDbUser)
            .addPair(ParamsEnum.peDbPwd)
            .addPair('-uccode', ucCode)
        retVal = execScript(params)
        if (closure!=null)
            closure.call(retVal)
        notifyAbout('Выполнено обновление базы данных', OP_UPDATE_DB, NOTIFY_TYPE_AFTER)
        retVal
    }

 }