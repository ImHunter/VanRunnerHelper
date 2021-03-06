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
     * Вид операции - неопределено (=0).
     */
    final static int OP_UNDEFINED = 0
    /**
     * Вид операции - запуск в режиме 1С:Предприятие (=1)
     */
    final static int OP_LAUNCH_USER_INTERFACE = 1
    /**
     * Вид опреации - блокировка/разблокировка пользовательских сеансов (=2)
     */
    final static int OP_SET_LOCK_USERS = 2
    /**
     * Вид операции - блокировка/разблокировка регламентных заданий (=3)
     */
    final static int OP_SET_LOCK_BACKGROUNDS = 3
    /**
     * Вид операции - завершение сеансов (=4)
     */
    final static int OP_KILL_SESSIONS = 4
    /**
     * Вид операции - обновление конфигурации из пакета обновления (=5)
     */
    final static int OP_UPDATE_CONFIG_FROM_PACKAGE = 5
    /**
     * Вид операции - обновление конфигурации из хранилища (=6)
     */
    final static int OP_UPDATE_CONFIG_FROM_REPO = 6
    /**
     * Вид операции - отключение конфигурации от хранилища (=7)
     */
    final static int OP_UNBIND_REPO = 7
    /**
     * Вид операции - обновление БД (=8)
     */
    final static int OP_UPDATE_DB = 8
    /**
     * Операция - ожидание завершения сессий (=9)
     */
    final static int OP_WAIT_FOR_CLOSE = 9
    /**
     * Операция - ожидание завершения сессий, выполнен цикл ожидания (=10)
     */
    final static int OP_WAIT_FOR_CLOSE_CONTINUE = 10
//    final static int OP_ =
//    final static int OP_ =
//    final static int OP_ =
    //endregion

    //region Поля public
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
    /**
     * Поле для хранения произвольного контекста.
     * Задумано для того, чтобы понимать, в каком контексте выполняется та или иная операция. Скорее всего, будет применяться для расширенной работы оповещалок через notifyEvent
     */
    protected def context
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

    // endregion

    // region Вложенные классы

    class ExecParams<String> extends ArrayList<String>{

        def params

        ExecParams(DeploykaHelper owner){
            super()
            this.params = owner.params
        }

        ExecParams(DeploykaHelper owner, DeplCommand command){
            super()
            this.params = owner.params
//            addValue(owner.pathToScript)
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

    // endregion

    DeploykaHelper(def paramScript, String pathToScript, String pathToServiceEPF = null){
        
        super(paramScript)

        this.pathToScript = qStr(pathToScript)

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
     * @param operationResult Результат выполнения операции
     * @param params Дополнительные параметры, которые могут быть переданы с оповещением.
     * Этим параметром передаются параметры выполнения какого-либо метода.
     * К примеру, при выполнении метода setLockStatusForUsers(locked), здесь передается параметр locked.
     */
    protected void notifyAbout(String msgText, int msgKind = OP_UNDEFINED, int msgType = NOTIFY_TYPE_UNDEFINED, def operationResult = null, Object... params){
        if (notifyEvent!=null)
            notifyEvent.call(msgText, this, msgKind, msgType, operationResult, params)
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
        testEcho("selfTest pathToScript: $pathToScript")

        setDbAuth('польззз', 'паророр')
        testEcho("executed setDbAuth('польззз', 'паророр')")

        params = new ExecParams(this)
        echo("test params new ExecParams(this): $params")

        params = new ExecParams(this)
        params.addValue(DeplCommand.dcRun)
        echo("test params new ExecParams(this) and params.addValue(VanRunnerCommand.dcRun): $params")

        params = new ExecParams(this, DeplCommand.dcRun)
        echo("test params new ExecParams(this, VanRunnerCommand.dcRun): $params")

        params = new ExecParams(this, DeplCommand.dcRun)
            .addPair(ParamsEnum.peDbServer)
            .addPair(ParamsEnum.peDbDatabase)
            .addPair(ParamsEnum.peDbUser)
            .addPair(ParamsEnum.peDbPwd)
            .addPair('custom key', 'custom value')

        echo("test params new ExecParams(this, VanRunnerCommand.dcRun) and many params: $params")

        launchUserInterface()
        echo("executed launchUserInterface")

        setRAS('ras server', 'ras utility')
        echo("executed setRAS()")

        setLockStatus(DeplCommand.dcSession, true)
        echo("executed setLockStatus(VanRunnerCommand.dcSession, true)")

//        killSessions(true, newSessionFilter().addAppFilter(AppNames.appClient, AppNames.appClientThin))
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
            .addAppFilter(AppNames.appClient, AppNames.appBackgroung, 'some app', 'other app')
            .setNamesFilter('админ', "польз")
            .toString()
        echo("Test filter filled: $flt")

        flt = newSessionFilter()
                .toString()
        echo("Test filter empty: $flt")

//        def now = Calendar.getInstance()
//        now.add(Calendar.MINUTE, 4)
//        echo("waitForCloseSessions: ${waitForCloseSessions(now.getTime())}")

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
        notifyAbout(opName, getOP_LAUNCH_USER_INTERFACE(), getNOTIFY_TYPE_BEFORE(), null, updateMetadata)
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
        notifyAbout(opName, getOP_LAUNCH_USER_INTERFACE(), getNOTIFY_TYPE_AFTER(), retVal, updateMetadata)
        retVal
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
        notifyAbout(msg, getOP_SET_LOCK_USERS(), getNOTIFY_TYPE_BEFORE(), null, locked)

        boolean retVal = setLockStatus(DeplCommand.dcSession, locked)

        msg = (locked ? 'Установка': 'Снятие') + ' блокировки сеансов пользователей ' +
                (retVal ? 'успешно' : 'не') + ' выполнена'
        notifyAbout(msg, getOP_SET_LOCK_USERS(), getNOTIFY_TYPE_AFTER(), retVal, locked)

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
        notifyAbout(msg, getOP_SET_LOCK_BACKGROUNDS(), getNOTIFY_TYPE_BEFORE(), null, locked)
        boolean retVal = setLockStatus(DeplCommand.dcScheduledJobs, locked)
        msg = (locked ? 'Установка': 'Снятие') + ' блокировки выполнения регламентных заданий ' +
                (retVal ? 'успешно' : 'не') + ' выполнена'
        notifyAbout(msg, getOP_SET_LOCK_BACKGROUNDS(), getNOTIFY_TYPE_AFTER(), retVal, locked)
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
            params = params.addValue("-with-nolock")
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

    /**
     * Обновление конфигурации из пакета обновлений
     * @param pathToPackage Путь к пакету обновлений
     * @return Результат обновления
     */
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
                .addPair("-uccode", ucCode)
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
    def findSessions(def appFilter = null, Closure closure = null) {
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
        def oper = OP_UPDATE_DB
        notifyAbout('Попытка обновления базы данных', oper, NOTIFY_TYPE_BEFORE)
        ExecParams params = new ExecParams(this, DeplCommand.dcUpdateDB)
            .addValue(ParamsEnum.peDbConnString)
            .addPair(ParamsEnum.peDbUser)
            .addPair(ParamsEnum.peDbPwd)
            .addValue('-allow-warnings')
            .addPair('-uccode', ucCode)
        retVal = execScript(params)
        if (closure!=null)
            closure.call(retVal)
        notifyAbout('Выполнено обновление базы данных', oper, NOTIFY_TYPE_AFTER)
        retVal
    }

    boolean waitForCloseSessions(Date maxDT, int minutesPerWaitCycle = 2, def appFilter = null){
        boolean retVal = !findSessions(appFilter)
        int oper = OP_WAIT_FOR_CLOSE
        if (!retVal) {
            int sleepTime = minutesPerWaitCycle>0 ? minutesPerWaitCycle * 60 * 1000 : maxDT - Date.newInstance()
            notifyAbout("Начало ожидания завершения процессов. Фильтр \"$appFilter\"", oper, NOTIFY_TYPE_BEFORE)
            int iter = 0
            while (Date.newInstance().compareTo(maxDT)<0) {
                iter++
                sleep(sleepTime)
                retVal = !findSessions(appFilter)
                if (retVal)
                    break
                else
                    notifyAbout("Продолжение ожидания завершения процессов. Цикл: $iter", OP_WAIT_FOR_CLOSE_CONTINUE, NOTIFY_TYPE_UNDEFINED)
            }
            if (!retVal)
                retVal = !findSessions(appFilter)
            notifyAbout('Ожидание завершения процессов окончено. Дождались: '.concat(retVal ? 'Да' : 'Нет'), oper, NOTIFY_TYPE_AFTER, maxDT, minutesPerWaitCycle, appFilter)
        }
        retVal
    }

 }