package Energos.Jenkins.OScript

import java.lang.*

/**
 * Класс-обертка для операций Vanessa-runner.
 */
class VanRunnerHelper extends OScriptHelper {

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
    /**
     * Операция - подключение конфигурации к хранилищу (=11)
     */
    final static int OP_BIND_REPO = 11
    /**
     * Запрос информации о БД через службу RAS (=12)
     */
    final static int OP_ASK_DATABASEINFO = 12
    /**
     * Выгрузка конфигурации БД (=13)
     */
    final static int OP_UNLOAD_CONFIG_DB = 13

//    final static int OP_ =
    //endregion

    // REGION Публичные поля

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

    public Map<String, String> databaseInfo = [:]
    /**
     * Поле для хранения произвольного контекста.
     * Задумано для того, чтобы понимать, в каком контексте выполняется та или иная операция. Скорее всего, будет применяться для расширенной работы оповещалок через notifyEvent
     */
    public def context
    /**
     * Значение по умолчанию признака, разрешено ли подключение сеансов.
     */
    public def sessionsEnabledDefault = true
    /**
     * Значение по умолчанию признака, разрешена ли работа РЗ.
     */
    public def scheduledJobsEnabledDefault = true

    public def printCmdOnce = true

    // endregion

    //endregion

    // region Перечисления

    /**
     * Перечисление с возможными командами Ванессы
     */
    enum VanRunnerCommand {
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
        dcUpdateCfg {
            @NonCPS
            @Override
            String toString() {return "update" }
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
         * Подключение конфигурации к хранилищу
         */
        dcBindRepo {
            @NonCPS
            @Override
            String toString() {return "bindrepo" }
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
                return "dbinfo"
            }
        },
        /**
         * Обновление БД
         */
        dcUpdateDB {
            @NonCPS
            @Override
            String toString() { return "updatedb" }
        },
        /**
         * Выгрузка конфигурации БД
         */
        dcUnloadConfigDB {
            @NonCPS
            @Override
            String toString() { return "unload" }
        }
    }

    /**
     * Перечисление в возможными параметрами запуска Ванессы
     */
    enum ParamsEnum {
        peDbServer,
        peDbConnString{
            @NonCPS
            @Override
            String toString() {return '--ibconnection' }
        },
        pePathToServiceEpf{
            @NonCPS
            @Override
            String toString() {return '--execute' }
        },
        peDbDatabase{
            @NonCPS
            @Override
            String toString() {return '--db' }
        },
        peDbUser{
            @NonCPS
            @Override
            String toString() {return '--db-user' }
        },
        peDbPwd{
            @NonCPS
            @Override
            String toString() {return '--db-pwd' }
        },
        peRepoPath{
            @NonCPS
            @Override
            String toString() {return '--storage-name' }
        },
        peRepoUser{
            @NonCPS
            @Override
            String toString() {return '--storage-user' }
        },
        peRepoPwd{
            @NonCPS
            @Override
            String toString() {return '--storage-pwd' }
        },
        peRepoVersion{
            @NonCPS
            @Override
            String toString() {return '--storage-ver' }
        },
        peLaunchCommand{
            @NonCPS
            @Override
            String toString() {return '--command' }
        },
        peLaunchAdditionalParams{
            @NonCPS
            @Override
            String toString() {return '--additional' }
        },
        peRASServer{
            @NonCPS
            @Override
            String toString() {return '--ras' }
        },
        peRACUtility{
            @NonCPS
            @Override
            String toString() {return '--rac' }
        },
        peSessionFilter{
            @NonCPS
            @Override
            String toString() {return '--filter' }
        },
        peUCCode{
            @NonCPS
            @Override
            String toString() {return '--uccode' }
        },
        peV8verion{
            @NonCPS
            @Override
            String toString() {return '--v8version' }
        },
        peClusterAdminName{
            @NonCPS
            @Override
            String toString() {return '--cluster-admin' }
        },
        peClusterAdminPwd{
            @NonCPS
            @Override
            String toString() {return '--cluster-pwd' }
        },
        peClientMode{
            @NonCPS
            @Override
            String toString() {return '--ordinaryapp' }
        },
        peKillWithNoLock{
            @NonCPS
            @Override
            String toString() { return '--with-nolock' }
        },
        peOnlineFile{
            @NonCPS
            @Override
            String toString() { return '--online-file' }
        }
    }

    // endregion

    // region Вложенные классы

    /**
     * Класс для формирования параметров запуска Ванессы
     */
    class ExecParams<String> extends ArrayList<String>{

        /**
         * Все параметры, заданные во владельце
         * @see VanRunnerHelper
         */
        def params

        /**
         * Конструктор объекта
         * @param owner Владелец, на основании которого будут заполняться параметры
         * @param command Опциональная команда, которую будет выполнять Ванесса
         * Эту команду можно и не передавать в конструкторе. Ее можно, например, добавить отдельно:<br>
         * {@code addCommand(VanRunnerCommand.dcUpdateCfg)} <br>
         * или {@code addValue(VanRunnerCommand.dcUpdateCfg)}
         */
        ExecParams(def owner, VanRunnerCommand command = null){
            super()
            this.params = owner.params
            addValue(command, command!=null)
        }

        /**
         * Добавить в параметры какое-либо значение
         * @param value Добавляемое значение
         * @param condition Условие, добавлять ли значение.
         * Условие служит для сокращения количества строк кода. Вместо <br>
         * {@code if (value>0) addValue(value)}
         * можем написать <br>
         * {@code addValue(value, value>0)}
         * @return Этот объект ExecParams
         */
        @NonCPS
        ExecParams addValue(def value, def condition = true) {
            if (condition==true){
                if (value == null) {
                    add(qStr())
                } else {
                    if (ParamsEnum.values().contains(value)) {
                        addValue(params.get(value), condition)
                    } else {
                        String strVal = "${value}".toString()
                        if (strVal.contains(' '))
                            strVal = qStr(strVal)
//                        if (condition==true)
                        add(strVal)
                    }
                }
            }
            this
        }

        /**
         * Добавление команды в параметры запуска.
         * В принципе, можно было обойтись использованием addValue(...). Но для некоторого улучшения читаемости добавление команды сделано отдельным методом.
         * @param command Добавляемая команда
         * @return Этот объект ExecParams
         */
        ExecParams addCommand(VanRunnerCommand command){
            addValue(command)
            this
        }

        /**
         * Добавление пары значений - строкового представления ключа и значения из поля params по этому ключу
         * @param param Ключ
         * @param condition Доп.условие, добавлять ли значения в параметры
         * @return Этот объект ExecParams
         */
        ExecParams addPair(ParamsEnum param, boolean condition = true) {
            if (condition)
                addValue(param.toString()).addValue(params.get(param))
            this
        }

        /**
         * Добавление пары значений - строкового ключа и его сткорового значения
         * @param parKey Ключ
         * @param parVal Значение ключа
         * @param condition Доп.условие, добавлять ли значения в параметры
         * @return Этот объект ExecParams
         */
        def addPair(String parKey, String parVal, boolean condition = true) {
            if (condition)
                addValue("$parKey".toString()).addValue(parVal)
            this
        }
    }

    // endregion

    /**
     * Конструктор
     * @param paramScript Объект script из Jenkins. В основном используется для echo.
     * @param pathToScript Путь к библиотеке Vanessa-runner
     * @param pathToServiceEPF Путь к сервисной внешней обработке. Используется лишь при запусках в режиме Предприятие для авто-завершения работы.
     */
    VanRunnerHelper(def paramScript, String pathToScript = null, String pathToServiceEPF = null){
        
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
        if (notifyEvent!=null) {
            notifyEvent.call(msgText, this, msgKind, msgType, operationResult, params)
            if (printCmdOnce==true)
                notifyEvent.call(launchString, this, msgKind, msgType, operationResult, params)
        }
    }

    /**
     * Метод для создания объекта, хранящего параметры запуска
     * @param command Опциональная команда, которая сразу добавляется в параметры запуска. Реализовано, в основном, для наглядности.
     * @return Созданный объект.
     */
    ExecParams newExecParams(VanRunnerCommand command = null) {
        new ExecParams(this, command)
    }

    /**
     * Метод для самотестирования библиотеки.
     * Это некая замена модульному тестированию - изначально были проблемы с запуском тестов. Приходилось тестировать таким вот образом.
     * Еще одна причина необходимости этой функции - это тестирование библиотеки в среде Jenkins на предмет необходимости NonCPS.
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
        params.addValue(VanRunnerCommand.dcRun)
        echo("test params new ExecParams(this) and params.addValue(VanRunnerCommand.dcRun): $params")

        params = new ExecParams(this, VanRunnerCommand.dcRun)
        echo("test params new ExecParams(this, VanRunnerCommand.dcRun): $params")

        params = new ExecParams(this, VanRunnerCommand.dcRun)
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

        setLockStatus(VanRunnerCommand.dcSession, true)
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
            .addAppBackgroung()
            .setNamesFilter('админ', "польз")
            .toString()
        echo("Test filter filled: $flt")

        flt = newSessionFilter()
                .toString()
        echo("Test filter empty: $flt")

        launchUserInterface(false){
            echo('launchUserInterface with closure')
        }

        echo("finish of selfTest")
        isTestMode = false
    }

    /**
     * Установка значения единичного параметра
     * @param paramKey Ключ параметра
     * @param paramValue Значение параметра
     * @param condition Условие, устанавливать ли значение.
     * Используется для сокращения количества строк
     * @return Этот объект VanRunnerHelper
     */
    @NonCPS
    def setParam(def paramKey, String paramValue, Boolean condition = true){
        if (condition==true) {
            this.params.put paramKey, paramValue
        }
        this
    }

    VanRunnerHelper setParam(Map<Object, String> newParams, ignoreEmptyValues = true){
        def filtered
        if (ignoreEmptyValues) {
            filtered = newParams.findAll { it.value != null }
        } else {
            filtered = newParams
        }
        params << filtered
        this
    }

    /**
     * Установка параметров подключения к БД
     * @param dbServer Сервер приложений
     * @param dbDatabase База данных
     * @param dbUser Имя пользователя конфигурации
     * @param dbPwd Пароль пользователя конфигурации
     * @param v8version Версия платформы
     * @return Этот объект VanRunnerHelper
     */
    VanRunnerHelper setDb(String dbServer, String dbDatabase, String dbUser = null, String dbPwd = null, String v8version = null) {
        setParam([(ParamsEnum.peDbDatabase): dbDatabase,
                  (ParamsEnum.peDbServer):dbServer,
                  (ParamsEnum.peDbUser):dbUser,
                  (ParamsEnum.peDbPwd):qStr(dbPwd),
                  (ParamsEnum.peV8verion):v8version])
        setParam((ParamsEnum.peDbConnString), "/S$dbServer\\$dbDatabase".toString())
        this
    }

    /**
     * Установка параметров подключения к БД с помощью строки соединения
     * @param connString Строка соединения ИБ
     * @param dbUser Имя пользователя ИБ (опционально)
     * @param dbPwd Пароль пользователя ИБ (опционально)
     * @param v8version Версия платформы (опционально)
     * @return Этот объект VanRunnerHelper
     */
    VanRunnerHelper setDbFromConnectString(String connString, String dbUser = null, String dbPwd = null, String v8version = null){
        // Srvr="upr-1cdevel:3041";Ref="oree_dolinin";
        def props
        def valServer = ''
        def valDB = ''
        def curVal
        connString.split(';').each {def partConnStr ->
            props = new Properties()
            props.load(new StringReader(partConnStr))
            // todo подумать и сделать регистр-независимо
            props.getProperty('?Srvr')?.with {valServer = it.replaceAll('"', ''); echo(it)}
            props.getProperty('Srvr')?.with {valServer = it.replaceAll('"', ''); echo(it)}
            props.getProperty('Ref')?.with {valDB = it.replaceAll('"', ''); echo(it)}
        }
        setDb(valServer, valDB, dbUser, dbPwd, v8version)
    }

    /**
     * Установка параметров аутентификации в БД 1С
     * @param dbUser Имя пользователя ИБ
     * @param dbPwd Пароль пользователя ИБ
     * @return Этот объект VanRunnerHelper
     */
    VanRunnerHelper setDbAuth(String dbUser, String dbPwd = null) {
        setParam([(ParamsEnum.peDbUser):ansi(dbUser),
                  (ParamsEnum.peDbPwd):qStr(ansi(dbPwd))
        ])
        this
    }

    /**
     * Установка параметров для подключения к хранилищу
     * @param repoPath Путь к хранилищу
     * @param repoUser Имя пользователя хранилища
     * @param repoPwd Пароль пользователя хранилища (опционально)
     * @return Этот объект VanRunnerHelper
     */
    VanRunnerHelper setRepo(String repoPath, String repoUser, String repoPwd = null) {
        setParam([(ParamsEnum.peRepoPath):ansi(repoPath),
                  (ParamsEnum.peRepoUser):ansi(repoUser),
                  (ParamsEnum.peRepoPwd):qStr(ansi(repoPwd))
        ])
        this
    }

    /**
     * Установка параметров аутентификации в хранилище
     * @param repoUser Имя пользователя хранилища
     * @param repoPwd Пароль пользователя хранилища (опционально)
     * @return
     */
    @NonCPS
    VanRunnerHelper setRepoAuth(String repoUser, String repoPwd = null) {
        setParam([(ParamsEnum.peRepoUser):ansi(repoUser),
                  (ParamsEnum.peRepoPwd):qStr(ansi(repoPwd))
        ])
        this
    }

    VanRunnerHelper setRAS(String rasServer, String racUtilPath, String clusterAdminName = null, String clusterAdminPwd = null) {
        setParam([(ParamsEnum.peRASServer):rasServer,
                  (ParamsEnum.peRACUtility):racUtilPath,
                  (ParamsEnum.peClusterAdminName):ansi(clusterAdminName),
                  (ParamsEnum.peClusterAdminPwd):qStr(ansi(clusterAdminPwd))
        ])
        this
    }

    VanRunnerHelper setDefaultEnables(def sessionsEnabledDefault = null, def scheduledJobsEnabledDefault = null, ucCode = null) {
        if (sessionsEnabledDefault!=null)
            this.sessionsEnabledDefault = sessionsEnabledDefault
        if (scheduledJobsEnabledDefault!=null)
            this.scheduledJobsEnabledDefault = scheduledJobsEnabledDefault
        if (ucCode!=null)
            this.ucCode = ucCode
        this
    }

    SessionFilter newSessionFilter(){
        new SessionFilter()
    }

    boolean launchUserInterface(boolean doUpdateMetadata = false, def launchMode = -1){

        resetResults()
        def retVal
        def opName = 'Запуск 1С:Предприятие'.concat( doUpdateMetadata ? ' (с обновлением метаданных)' : '')

        String launchParam = 'ЗавершитьРаботуСистемы;'.toString()
        if (doUpdateMetadata)
            launchParam = launchParam.concat('ЗапуститьОбновлениеИнформационнойБазы;'.toString())
        setParam( ParamsEnum.peLaunchCommand, qStr(utf8(launchParam)))
        testEcho('подготовили параметры запуска launchParam')

        notifyAbout(opName, getOP_LAUNCH_USER_INTERFACE(), getNOTIFY_TYPE_BEFORE(), null, doUpdateMetadata)
        retVal = execScript(
                new ExecParams(this, VanRunnerCommand.dcRun)
                .addPair(ParamsEnum.peDbConnString)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
                .addPair(ParamsEnum.peLaunchCommand)
                .addPair(ParamsEnum.pePathToServiceEpf)
                .addPair(ParamsEnum.peUCCode.toString(), ucCode, ucCode!=null && ucCode.empty==false)
                .addPair('--ordinaryapp', launchMode) // чтобы в ключах запуска не было RunModeManagedApplication, т.к. это гасит вывод лога
//                .addPair(ParamsEnum.peOnlineFile, "C:\\OScripts\\log.txt")
        )
        configInfo.readLogInfo(resultLog)
        echo(resultLog) // todo Потом убрать!
        notifyAbout(opName, getOP_LAUNCH_USER_INTERFACE(), getNOTIFY_TYPE_AFTER(), retVal, doUpdateMetadata)
        printCmdOnce = false
        retVal
    }

    private boolean setResourceEnabled(VanRunnerCommand resource, Boolean isEnabled){
        String op = isEnabled ? 'unlock' : 'lock'
        ExecParams params = new ExecParams(this, resource)
                .addValue(op)
                .addPair(ParamsEnum.peRASServer)
                .addPair(ParamsEnum.peRACUtility)
                .addPair(ParamsEnum.peDbDatabase)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
                .addPair(ParamsEnum.peUCCode.toString(), ucCode, resource==VanRunnerCommand.dcSession && ucCode!=null)
        execScript(params)
    }

    boolean setSessionsEnabled(Boolean isEnabled = null) {

        resetResults()
        boolean enabledValue = isEnabled==null ? sessionsEnabledDefault : isEnabled

        String msg = 'Попытка ' + (enabledValue ? 'разрешения': 'запрета') + ' сеансов приложений'
        notifyAbout(msg, getOP_SET_LOCK_USERS(), getNOTIFY_TYPE_BEFORE(), null, enabledValue)

        boolean retVal = setResourceEnabled(VanRunnerCommand.dcSession, enabledValue)

        msg = (enabledValue ? 'Разрешение': 'Запрет') + ' сеансов приложений - ' +
                (retVal ? 'успешно' : 'не') + ' выполнено'
        notifyAbout(msg, getOP_SET_LOCK_USERS(), getNOTIFY_TYPE_AFTER(), retVal, enabledValue)

        retVal
    }

    boolean setBackgroundsEnabled(Boolean isEnabled = null) {

        resetResults()
        boolean enabledValue = isEnabled==null ? scheduledJobsEnabledDefault : isEnabled
        String msg = 'Попытка ' + (enabledValue ? 'разрешения': 'запрета') + ' выполнения регламентных заданий'
        notifyAbout(msg, getOP_SET_LOCK_BACKGROUNDS(), getNOTIFY_TYPE_BEFORE(), null, enabledValue)

        boolean retVal = setResourceEnabled(VanRunnerCommand.dcScheduledJobs, enabledValue )

        msg = (enabledValue ? 'Разрешение': 'Запрет') + ' выполнения регламентных заданий - ' +
                (retVal ? 'успешно' : 'не') + ' выполнено'
        notifyAbout(msg, getOP_SET_LOCK_BACKGROUNDS(), getNOTIFY_TYPE_AFTER(), retVal, enabledValue)
        printCmdOnce = false
        retVal
    }

    /**
     * Принудительное завершение сеансов
     * @param withNoLock Не блокировать начало сеансов после их принудительного завершения
     * @param appFilter Фильтр, с помощью которого можно завершать не все сеансы, а согласно каким-то условиям.
     * Параметр appFilter можно создать с помощью объекта типа SessionFilter. Пример:
     * {@code newSessionFilter().addAppDesigner()}
     * @return Булево - успешно ли выполнилась операция.
     * @see SessionFilter
     */
    def killSessions(Boolean withNoLock = true, def appFilter = '', def attemptsCount = 5) {
        resetResults()
        String filter = appFilter.toString()
        String msg = 'Попытка завершения сеансов' + (appFilter==null || filter.isEmpty() ? '' : '; фильтр: ' + filter)
        int oper = OP_KILL_SESSIONS
        notifyAbout(msg, oper, getNOTIFY_TYPE_BEFORE())

        ExecParams params = new ExecParams(this, VanRunnerCommand.dcSession)
                .addValue('kill')
                .addPair(ParamsEnum.peRASServer)
                .addPair(ParamsEnum.peRACUtility)
                .addPair(ParamsEnum.peDbDatabase)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
                .addPair(ParamsEnum.peUCCode.toString(), ucCode, ucCode!=null)
                .addPair('--try', attemptsCount.toString(), attemptsCount!=null && attemptsCount>0)
                .addValue(ParamsEnum.peKillWithNoLock.toString(), withNoLock)
                .addPair(ParamsEnum.peSessionFilter, filter, appFilter!=null && !filter.isEmpty())

        notifyAbout("Параметры запуска при прерывании сеансов: $params".toString(), oper, getNOTIFY_TYPE_BEFORE())
        boolean retVal = execScript(params)
        msg = 'Завершение сеансов '.concat(retVal ? 'успешно' : 'не').concat(' выполнено').concat(appFilter==null || filter.isEmpty() ? '' : '; фильтр: ' + filter)
        notifyAbout(msg, oper, getNOTIFY_TYPE_AFTER(), retVal)
        printCmdOnce = false
        retVal
    }

    /**
     * Обновление конфигурации из пакета обновлений
     * @param pathToPackage Путь к пакету обновлений
     * @return Результат обновления
     */
    boolean updateConfigFromPackage(String pathToPackage) {
        String msg = 'Попытка обновления конфигурации из пакета обновлений'
        resetResults()
        notifyAbout(msg, getOP_UPDATE_CONFIG_FROM_PACKAGE(), getNOTIFY_TYPE_BEFORE(), pathToPackage)
        boolean retVal = execScript(
                new ExecParams(this)
                .addCommand(VanRunnerCommand.dcUpdateCfg)
                .addPair(ParamsEnum.peDbConnString) // todo Не уверен
                .addValue(pathToPackage)
//                .addPair(ParamsEnum.peConfigUpdateMode, "-auto")
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
                .addPair(ParamsEnum.peUCCode, ucCode, ucCode!=null)
        )
        notifyAbout(msg, getOP_UPDATE_CONFIG_FROM_PACKAGE(), getNOTIFY_TYPE_AFTER(), retVal, pathToPackage)
        retVal
    }

    boolean updateConfigFromPackage(String pathToPackage, Closure closure) {
        def retVal = updateConfigFromPackage(pathToPackage)
        closure(retVal)
        retVal
    }

    def loadConfigFromRepo(def repoVersion = null) {

        resetResults()
        String msg = 'Попытка обновления конфигурации из хранилища'
        notifyAbout(msg, getOP_UPDATE_CONFIG_FROM_REPO(), getNOTIFY_TYPE_BEFORE())
        def retVal = execScript(
                new ExecParams(this)
                .addCommand(VanRunnerCommand.dcLoadRepo)
                .addPair(ParamsEnum.peDbConnString)
                .addPair(ParamsEnum.peRepoPath)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
                .addPair(ParamsEnum.peRepoUser)
                .addPair(ParamsEnum.peRepoPwd)
//                .addPair(ParamsEnum.peUCCode, ucCode, ucCode!=null)
                .addPair(ParamsEnum.peRepoVersion, repoVersion, repoVersion!=null)
        )
        msg = 'Обновление конфигурации из хранилища ' + (retVal ? 'успешно' : 'не') + ' выполнено'
        notifyAbout(msg, getOP_UPDATE_CONFIG_FROM_REPO(), getNOTIFY_TYPE_AFTER(), retVal)
        printCmdOnce = false
        retVal
    }

    def bindRepo(def bindAlreadyBindedUser = true, replaceCfg = true) {
        def msg = 'Попытка подключения конфигурации к хранилищу'
        resetResults()
        notifyAbout(msg, OP_BIND_REPO, getNOTIFY_TYPE_BEFORE())
        def retVal = execScript(
                newExecParams(VanRunnerCommand.dcBindRepo)
                        .addValue(ParamsEnum.peRepoPath)
                        .addValue(ParamsEnum.peRepoUser)
                        .addValue(ParamsEnum.peRepoPwd)
                        .addValue('--BindAlreadyBindedUser', bindAlreadyBindedUser==true)
                        .addValue('--NotReplaceCfg', replaceCfg==true) // в Ванессе ключ NotReplaceCfg работает нелогично. Подстраиваюсь под его логику.
                        .addPair(ParamsEnum.peDbConnString) // todo Не уверен
                        .addPair(ParamsEnum.peDbUser)
                        .addPair(ParamsEnum.peDbPwd)
        )
        msg = 'Подключение конфигурации к хранилищу ' + (retVal ? 'успешно' : 'не') + ' выполнено'
        notifyAbout(msg, OP_BIND_REPO, getNOTIFY_TYPE_AFTER(), retVal)
        retVal
    }

    def unbindRepo() {
        def msg = 'Попытка отключения конфигурации от хранилища'
        resetResults()
        notifyAbout(msg, getOP_UNBIND_REPO(), getNOTIFY_TYPE_BEFORE())
        def retVal = execScript(
                new ExecParams(this)
                .addCommand(VanRunnerCommand.dcUnbindRepo)
                .addPair(ParamsEnum.peDbConnString) // todo Не уверен
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
        )
        msg = 'Отключение конфигурации от хранилища ' + (retVal ? 'успешно' : 'не') + ' выполнено'
        notifyAbout(msg, getOP_UNBIND_REPO(), getNOTIFY_TYPE_AFTER(), retVal)
        retVal
    }

    // возврат Истина, если сеансы найдены
    def isSessionsClosed(def appFilter = null, Closure closure = null) {
        ExecParams params = new ExecParams(this)
                .addCommand(VanRunnerCommand.dcSession)
                .addValue('closed')
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
        retVal
    }

    boolean updateDb(String addParams = null) {
        // В самой Ванессе, на самом деле, дополнительные параметры запуска не используются.
        resetResults()
        boolean retVal
        def oper = OP_UPDATE_DB
        notifyAbout('Попытка обновления базы данных', oper, NOTIFY_TYPE_BEFORE)
        ExecParams params = new ExecParams(this, VanRunnerCommand.dcUpdateDB)
            .addPair(ParamsEnum.peDbConnString)
            .addPair(ParamsEnum.peDbUser)
            .addPair(ParamsEnum.peDbPwd)
            .addPair(ParamsEnum.peUCCode, ucCode, ucCode!=null)

        retVal = execScript(params)
        notifyAbout('Выполнено обновление базы данных', oper, NOTIFY_TYPE_AFTER, retVal)
        printCmdOnce = false
        retVal
    }

//    @NonCPS
    boolean waitForCloseSessions(def maxDT, int minutesPerWaitCycle = 2, def appFilter = null){
        resetResults()
        def oper = OP_WAIT_FOR_CLOSE
        def retVal = true
        notifyAbout("Попытка ожидания завершения процессов. Фильтр '${appFilter}'; ждем до ${maxDT.toString()} с периодом ${minutesPerWaitCycle.toString()} мин", oper, NOTIFY_TYPE_BEFORE)
        retVal = isSessionsClosed(appFilter)
        if (!retVal) {
            int sleepTime = minutesPerWaitCycle>0 ? minutesPerWaitCycle * 60 * 1000 : maxDT - Date.newInstance()
            notifyAbout("Начало ожидания завершения процессов. Фильтр \"$appFilter\"", oper, NOTIFY_TYPE_BEFORE)
            int iter = 0
            while (Date.newInstance() < maxDT) {
                iter++
                sleep(sleepTime)
                retVal = isSessionsClosed(appFilter)
                if (retVal)
                    break
                else
                    notifyAbout("Продолжение ожидания завершения процессов. Цикл: $iter", OP_WAIT_FOR_CLOSE_CONTINUE, NOTIFY_TYPE_UNDEFINED)
            }
            if (!retVal)
                retVal = isSessionsClosed(appFilter)
        }
        notifyAbout('Ожидание завершения процессов окончено. Дождались: '.concat(retVal ? 'Да' : 'Нет'), oper, NOTIFY_TYPE_AFTER, retVal, maxDT, minutesPerWaitCycle, appFilter)
        retVal
    }

    boolean askDatabaseInfo(){
        resetResults()
        boolean retVal
        def oper = OP_ASK_DATABASEINFO
        notifyAbout('Попытка запроса информации о базе данных', oper, NOTIFY_TYPE_BEFORE)
        ExecParams params = new ExecParams(this, VanRunnerCommand.dcInfo)
                .addPair(ParamsEnum.peRASServer)
                .addPair(ParamsEnum.peRACUtility)
                .addPair(ParamsEnum.peDbDatabase)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
        retVal = execScript(params)
        DatabaseInfoReader.readInfo(resultLog, databaseInfo)
        notifyAbout('Информация о базе данных '.concat(retVal==true ? 'успешно' : 'не').concat(' прочитана'), oper, NOTIFY_TYPE_AFTER, retVal)
        retVal
    }

    boolean unloadConfigDB(def resultFileName){
        resetResults()
        boolean retVal
        def oper = OP_UNLOAD_CONFIG_DB
        notifyAbout('Попытка выгрузки конфигурации БД', oper, NOTIFY_TYPE_BEFORE)
        ExecParams params = new ExecParams(this, VanRunnerCommand.dcUnloadConfigDB)
                .addValue(resultFileName)
                .addPair(ParamsEnum.peDbConnString)
//                .addPair(ParamsEnum.peDbDatabase)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
        retVal = execScript(params)
        notifyAbout('Выгрузка конфигурации БД '.concat(retVal==true ? 'успешно' : 'не').concat(' выполнена'), oper, NOTIFY_TYPE_AFTER, retVal)
        retVal
    }

}