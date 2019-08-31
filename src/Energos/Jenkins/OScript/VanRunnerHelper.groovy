package Energos.Jenkins.OScript

import java.lang.*

/**
 * �����-������� ��� �������� Vanessa-runner.
 */
class VanRunnerHelper extends OScriptHelper {

    //region ��������� ����� ����������
    /**
     * ��� ���������� - ������������
     */
    final static int NOTIFY_TYPE_UNDEFINED = 0
    /**
     * ��� ���������� - ����� ����������� ��������
     */
    final static int NOTIFY_TYPE_BEFORE = 1
    /**
     * ��� ���������� - ����� ���������� ��������
     */
    final static int NOTIFY_TYPE_AFTER = 2
    //endregion

    //region ��������� ����� ���������� (��������)
    /**
     * ��� �������� - ������������ (=0).
     */
    final static int OP_UNDEFINED = 0
    /**
     * ��� �������� - ������ � ������ 1�:����������� (=1)
     */
    final static int OP_LAUNCH_USER_INTERFACE = 1
    /**
     * ��� �������� - ����������/������������� ���������������� ������� (=2)
     */
    final static int OP_SET_LOCK_USERS = 2
    /**
     * ��� �������� - ����������/������������� ������������ ������� (=3)
     */
    final static int OP_SET_LOCK_BACKGROUNDS = 3
    /**
     * ��� �������� - ���������� ������� (=4)
     */
    final static int OP_KILL_SESSIONS = 4
    /**
     * ��� �������� - ���������� ������������ �� ������ ���������� (=5)
     */
    final static int OP_UPDATE_CONFIG_FROM_PACKAGE = 5
    /**
     * ��� �������� - ���������� ������������ �� ��������� (=6)
     */
    final static int OP_UPDATE_CONFIG_FROM_REPO = 6
    /**
     * ��� �������� - ���������� ������������ �� ��������� (=7)
     */
    final static int OP_UNBIND_REPO = 7
    /**
     * ��� �������� - ���������� �� (=8)
     */
    final static int OP_UPDATE_DB = 8
    /**
     * �������� - �������� ���������� ������ (=9)
     */
    final static int OP_WAIT_FOR_CLOSE = 9
    /**
     * �������� - �������� ���������� ������, �������� ���� �������� (=10)
     */
    final static int OP_WAIT_FOR_CLOSE_CONTINUE = 10
    /**
     * �������� - ����������� ������������ � ��������� (=11)
     */
    final static int OP_BIND_REPO = 11
    /**
     * ������ ���������� � �� ����� ������ RAS (=12)
     */
    final static int OP_ASK_DATABASEINFO = 12
    /**
     * �������� ������������ �� (=13)
     */
    final static int OP_UNLOAD_CONFIG_DB = 13

//    final static int OP_ =
    //endregion

    // REGION ��������� ����

    /**
     * ��������, ������� ����� ���� ������������ ��� ���������� �������.
     * �������� �������� set... (��������, setDb(...)).
     */
    public Map<Object, String> params = [:]
    /**
     * ��������, ������� ����� ���� �������, ��������, ��� ���������� �������. �.�., ��, ��� ���������� � ����� /UC
     */
    public String ucCode = 'blocked'
    /**
     * Closure, ������� ����� ���� ������������ ��� ����������� ��������. ���������� ������ ������ notifyAbout().
     */
    public Closure notifyEvent = null
    /**
     * ������, ���������� ���������� � ������������.
     * ���������� ����������� ��� ���������� ������� 1� � ������ �����������. ��� ����, ���������� ������ ������������������
     * ������� ���������. ���������� �� ������ (���) ����������� � ����������������.
     */
    public ConfigInfo configInfo

    public Map<String, String> databaseInfo = [:]
    /**
     * ���� ��� �������� ������������� ���������.
     * �������� ��� ����, ����� ��������, � ����� ��������� ����������� �� ��� ���� ��������. ������ �����, ����� ����������� ��� ����������� ������ ���������� ����� notifyEvent
     */
    public def context
    /**
     * �������� �� ��������� ��������, ��������� �� ����������� �������.
     */
    public def sessionsEnabledDefault = true
    /**
     * �������� �� ��������� ��������, ��������� �� ������ ��.
     */
    public def scheduledJobsEnabledDefault = true

    public def printCmdOnce = true

    // endregion

    //endregion

    // region ������������

    /**
     * ������������ � ���������� ��������� �������
     */
    enum VanRunnerCommand {
        /**
         * ������ � ������ �����������
         */
        dcRun {
            @NonCPS
            @Override
            String toString() { return "run" }
        },
        /**
         * ���������� ������������ �� ������ ����������
         */
        dcUpdateCfg {
            @NonCPS
            @Override
            String toString() {return "update" }
        },
        /**
         * ���������� ������������ �� ���������
         */
        dcLoadRepo {
            @NonCPS
            @Override
            String toString() {return "loadrepo" }
        },
        /**
         * ����������� ������������ � ���������
         */
        dcBindRepo {
            @NonCPS
            @Override
            String toString() {return "bindrepo" }
        },
        /**
         * ���������� ������������ �� ���������
         */
        dcUnbindRepo {
            @NonCPS
            @Override
            String toString() {return "unbindrepo" }
        },
        /**
         * �������� � ��������
         */
        dcSession {
            @NonCPS
            @Override
            String toString() {return "session" }
        },
        /**
         * �������� � ������������� ���������
         */
        dcScheduledJobs {
            @NonCPS
            @Override
            String toString() {return "scheduledjobs" }
        },
        /**
         * ��������� ���������� � ���� ������
         */
        dcInfo {
            @NonCPS
            @Override
            String toString() {
                return "dbinfo"
            }
        },
        /**
         * ���������� ��
         */
        dcUpdateDB {
            @NonCPS
            @Override
            String toString() { return "updatedb" }
        },
        /**
         * �������� ������������ ��
         */
        dcUnloadConfigDB {
            @NonCPS
            @Override
            String toString() { return "unload" }
        }
    }

    /**
     * ������������ � ���������� ����������� ������� �������
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

    // region ��������� ������

    /**
     * ����� ��� ������������ ���������� ������� �������
     */
    class ExecParams<String> extends ArrayList<String>{

        /**
         * ��� ���������, �������� �� ���������
         * @see VanRunnerHelper
         */
        def params

        /**
         * ����������� �������
         * @param owner ��������, �� ��������� �������� ����� ����������� ���������
         * @param command ������������ �������, ������� ����� ��������� �������
         * ��� ������� ����� � �� ���������� � ������������. �� �����, ��������, �������� ��������:<br>
         * {@code addCommand(VanRunnerCommand.dcUpdateCfg)} <br>
         * ��� {@code addValue(VanRunnerCommand.dcUpdateCfg)}
         */
        ExecParams(def owner, VanRunnerCommand command = null){
            super()
            this.params = owner.params
            addValue(command, command!=null)
        }

        /**
         * �������� � ��������� �����-���� ��������
         * @param value ����������� ��������
         * @param condition �������, ��������� �� ��������.
         * ������� ������ ��� ���������� ���������� ����� ����. ������ <br>
         * {@code if (value>0) addValue(value)}
         * ����� �������� <br>
         * {@code addValue(value, value>0)}
         * @return ���� ������ ExecParams
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
         * ���������� ������� � ��������� �������.
         * � ��������, ����� ���� �������� �������������� addValue(...). �� ��� ���������� ��������� ���������� ���������� ������� ������� ��������� �������.
         * @param command ����������� �������
         * @return ���� ������ ExecParams
         */
        ExecParams addCommand(VanRunnerCommand command){
            addValue(command)
            this
        }

        /**
         * ���������� ���� �������� - ���������� ������������� ����� � �������� �� ���� params �� ����� �����
         * @param param ����
         * @param condition ���.�������, ��������� �� �������� � ���������
         * @return ���� ������ ExecParams
         */
        ExecParams addPair(ParamsEnum param, boolean condition = true) {
            if (condition)
                addValue(param.toString()).addValue(params.get(param))
            this
        }

        /**
         * ���������� ���� �������� - ���������� ����� � ��� ���������� ��������
         * @param parKey ����
         * @param parVal �������� �����
         * @param condition ���.�������, ��������� �� �������� � ���������
         * @return ���� ������ ExecParams
         */
        def addPair(String parKey, String parVal, boolean condition = true) {
            if (condition)
                addValue("$parKey".toString()).addValue(parVal)
            this
        }
    }

    // endregion

    /**
     * �����������
     * @param paramScript ������ script �� Jenkins. � �������� ������������ ��� echo.
     * @param pathToScript ���� � ���������� Vanessa-runner
     * @param pathToServiceEPF ���� � ��������� ������� ���������. ������������ ���� ��� �������� � ������ ����������� ��� ����-���������� ������.
     */
    VanRunnerHelper(def paramScript, String pathToScript = null, String pathToServiceEPF = null){
        
        super(paramScript)

        this.pathToScript = qStr(pathToScript)

        setParam(ParamsEnum.pePathToServiceEpf, qStr(pathToServiceEPF), pathToServiceEPF!=null)
        configInfo = new ConfigInfo()

    }

    /**
     * ����� ��� ���������� � �����-���� �������.
     * �������� ���������� notifyEvent, ���� ��� Closure ������.
     * � notifyEvent ���������� ��������� ����������: msgText - ���������� ���������; ������� ������ this, msgKind, msgType, params.
     * ����� ����������� �������� ��� ����, ����� ����� ���� ���������� ������ ������������ ��������� ����������. � ��� ������������� -
     * �������� ��� ��������� ���������� �������� ������������ ��������� � ������� ����������.
     * @param msgText ���������� ���������.
     * @param msgKind ��� ��������, � ������� ���������� ����������.
     * ������������ �������� �������� OP_*
     * @param msgType ��� ���������� (��, �����, �� ������).
     * ������������ �������� �������� NOTIFY_TYPE_*
     * @param operationResult ��������� ���������� ��������
     * @param params �������������� ���������, ������� ����� ���� �������� � �����������.
     * ���� ���������� ���������� ��������� ���������� ������-���� ������.
     * � �������, ��� ���������� ������ setLockStatusForUsers(locked), ����� ���������� �������� locked.
     */
    protected void notifyAbout(String msgText, int msgKind = OP_UNDEFINED, int msgType = NOTIFY_TYPE_UNDEFINED, def operationResult = null, Object... params){
        if (notifyEvent!=null) {
            notifyEvent.call(msgText, this, msgKind, msgType, operationResult, params)
            if (printCmdOnce==true)
                notifyEvent.call(launchString, this, msgKind, msgType, operationResult, params)
        }
    }

    /**
     * ����� ��� �������� �������, ��������� ��������� �������
     * @param command ������������ �������, ������� ����� ����������� � ��������� �������. �����������, � ��������, ��� �����������.
     * @return ��������� ������.
     */
    ExecParams newExecParams(VanRunnerCommand command = null) {
        new ExecParams(this, command)
    }

    /**
     * ����� ��� ���������������� ����������.
     * ��� ����� ������ ���������� ������������ - ���������� ���� �������� � �������� ������. ����������� ����������� ����� ��� �������.
     * ��� ���� ������� ������������� ���� ������� - ��� ������������ ���������� � ����� Jenkins �� ������� ������������� NonCPS.
     * ��������������� ���� ������������ (isTestMode = true), ����������� ����� ��������, ���� ������������.
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

        setDbAuth('�������', '�������')
        testEcho("executed setDbAuth('�������', '�������')")

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
            .setNamesFilter('�����', "�����")
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
     * ��������� �������� ���������� ���������
     * @param paramKey ���� ���������
     * @param paramValue �������� ���������
     * @param condition �������, ������������� �� ��������.
     * ������������ ��� ���������� ���������� �����
     * @return ���� ������ VanRunnerHelper
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
     * ��������� ���������� ����������� � ��
     * @param dbServer ������ ����������
     * @param dbDatabase ���� ������
     * @param dbUser ��� ������������ ������������
     * @param dbPwd ������ ������������ ������������
     * @param v8version ������ ���������
     * @return ���� ������ VanRunnerHelper
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
     * ��������� ���������� ����������� � �� � ������� ������ ����������
     * @param connString ������ ���������� ��
     * @param dbUser ��� ������������ �� (�����������)
     * @param dbPwd ������ ������������ �� (�����������)
     * @param v8version ������ ��������� (�����������)
     * @return ���� ������ VanRunnerHelper
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
            // todo �������� � ������� �������-����������
            props.getProperty('?Srvr')?.with {valServer = it.replaceAll('"', ''); echo(it)}
            props.getProperty('Srvr')?.with {valServer = it.replaceAll('"', ''); echo(it)}
            props.getProperty('Ref')?.with {valDB = it.replaceAll('"', ''); echo(it)}
        }
        setDb(valServer, valDB, dbUser, dbPwd, v8version)
    }

    /**
     * ��������� ���������� �������������� � �� 1�
     * @param dbUser ��� ������������ ��
     * @param dbPwd ������ ������������ ��
     * @return ���� ������ VanRunnerHelper
     */
    VanRunnerHelper setDbAuth(String dbUser, String dbPwd = null) {
        setParam([(ParamsEnum.peDbUser):ansi(dbUser),
                  (ParamsEnum.peDbPwd):qStr(ansi(dbPwd))
        ])
        this
    }

    /**
     * ��������� ���������� ��� ����������� � ���������
     * @param repoPath ���� � ���������
     * @param repoUser ��� ������������ ���������
     * @param repoPwd ������ ������������ ��������� (�����������)
     * @return ���� ������ VanRunnerHelper
     */
    VanRunnerHelper setRepo(String repoPath, String repoUser, String repoPwd = null) {
        setParam([(ParamsEnum.peRepoPath):ansi(repoPath),
                  (ParamsEnum.peRepoUser):ansi(repoUser),
                  (ParamsEnum.peRepoPwd):qStr(ansi(repoPwd))
        ])
        this
    }

    /**
     * ��������� ���������� �������������� � ���������
     * @param repoUser ��� ������������ ���������
     * @param repoPwd ������ ������������ ��������� (�����������)
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
        def opName = '������ 1�:�����������'.concat( doUpdateMetadata ? ' (� ����������� ����������)' : '')

        String launchParam = '����������������������;'.toString()
        if (doUpdateMetadata)
            launchParam = launchParam.concat('�������������������������������������;'.toString())
        setParam( ParamsEnum.peLaunchCommand, qStr(utf8(launchParam)))
        testEcho('����������� ��������� ������� launchParam')

        notifyAbout(opName, getOP_LAUNCH_USER_INTERFACE(), getNOTIFY_TYPE_BEFORE(), null, doUpdateMetadata)
        retVal = execScript(
                new ExecParams(this, VanRunnerCommand.dcRun)
                .addPair(ParamsEnum.peDbConnString)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
                .addPair(ParamsEnum.peLaunchCommand)
                .addPair(ParamsEnum.pePathToServiceEpf)
                .addPair(ParamsEnum.peUCCode.toString(), ucCode, ucCode!=null && ucCode.empty==false)
                .addPair('--ordinaryapp', launchMode) // ����� � ������ ������� �� ���� RunModeManagedApplication, �.�. ��� ����� ����� ����
//                .addPair(ParamsEnum.peOnlineFile, "C:\\OScripts\\log.txt")
        )
        configInfo.readLogInfo(resultLog)
        echo(resultLog) // todo ����� ������!
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

        String msg = '������� ' + (enabledValue ? '����������': '�������') + ' ������� ����������'
        notifyAbout(msg, getOP_SET_LOCK_USERS(), getNOTIFY_TYPE_BEFORE(), null, enabledValue)

        boolean retVal = setResourceEnabled(VanRunnerCommand.dcSession, enabledValue)

        msg = (enabledValue ? '����������': '������') + ' ������� ���������� - ' +
                (retVal ? '�������' : '��') + ' ���������'
        notifyAbout(msg, getOP_SET_LOCK_USERS(), getNOTIFY_TYPE_AFTER(), retVal, enabledValue)

        retVal
    }

    boolean setBackgroundsEnabled(Boolean isEnabled = null) {

        resetResults()
        boolean enabledValue = isEnabled==null ? scheduledJobsEnabledDefault : isEnabled
        String msg = '������� ' + (enabledValue ? '����������': '�������') + ' ���������� ������������ �������'
        notifyAbout(msg, getOP_SET_LOCK_BACKGROUNDS(), getNOTIFY_TYPE_BEFORE(), null, enabledValue)

        boolean retVal = setResourceEnabled(VanRunnerCommand.dcScheduledJobs, enabledValue )

        msg = (enabledValue ? '����������': '������') + ' ���������� ������������ ������� - ' +
                (retVal ? '�������' : '��') + ' ���������'
        notifyAbout(msg, getOP_SET_LOCK_BACKGROUNDS(), getNOTIFY_TYPE_AFTER(), retVal, enabledValue)
        printCmdOnce = false
        retVal
    }

    /**
     * �������������� ���������� �������
     * @param withNoLock �� ����������� ������ ������� ����� �� ��������������� ����������
     * @param appFilter ������, � ������� �������� ����� ��������� �� ��� ������, � �������� �����-�� ��������.
     * �������� appFilter ����� ������� � ������� ������� ���� SessionFilter. ������:
     * {@code newSessionFilter().addAppDesigner()}
     * @return ������ - ������� �� ����������� ��������.
     * @see SessionFilter
     */
    def killSessions(Boolean withNoLock = true, def appFilter = '', def attemptsCount = 5) {
        resetResults()
        String filter = appFilter.toString()
        String msg = '������� ���������� �������' + (appFilter==null || filter.isEmpty() ? '' : '; ������: ' + filter)
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

        notifyAbout("��������� ������� ��� ���������� �������: $params".toString(), oper, getNOTIFY_TYPE_BEFORE())
        boolean retVal = execScript(params)
        msg = '���������� ������� '.concat(retVal ? '�������' : '��').concat(' ���������').concat(appFilter==null || filter.isEmpty() ? '' : '; ������: ' + filter)
        notifyAbout(msg, oper, getNOTIFY_TYPE_AFTER(), retVal)
        printCmdOnce = false
        retVal
    }

    /**
     * ���������� ������������ �� ������ ����������
     * @param pathToPackage ���� � ������ ����������
     * @return ��������� ����������
     */
    boolean updateConfigFromPackage(String pathToPackage) {
        String msg = '������� ���������� ������������ �� ������ ����������'
        resetResults()
        notifyAbout(msg, getOP_UPDATE_CONFIG_FROM_PACKAGE(), getNOTIFY_TYPE_BEFORE(), pathToPackage)
        boolean retVal = execScript(
                new ExecParams(this)
                .addCommand(VanRunnerCommand.dcUpdateCfg)
                .addPair(ParamsEnum.peDbConnString) // todo �� ������
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
        String msg = '������� ���������� ������������ �� ���������'
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
        msg = '���������� ������������ �� ��������� ' + (retVal ? '�������' : '��') + ' ���������'
        notifyAbout(msg, getOP_UPDATE_CONFIG_FROM_REPO(), getNOTIFY_TYPE_AFTER(), retVal)
        printCmdOnce = false
        retVal
    }

    def bindRepo(def bindAlreadyBindedUser = true, replaceCfg = true) {
        def msg = '������� ����������� ������������ � ���������'
        resetResults()
        notifyAbout(msg, OP_BIND_REPO, getNOTIFY_TYPE_BEFORE())
        def retVal = execScript(
                newExecParams(VanRunnerCommand.dcBindRepo)
                        .addValue(ParamsEnum.peRepoPath)
                        .addValue(ParamsEnum.peRepoUser)
                        .addValue(ParamsEnum.peRepoPwd)
                        .addValue('--BindAlreadyBindedUser', bindAlreadyBindedUser==true)
                        .addValue('--NotReplaceCfg', replaceCfg==true) // � ������� ���� NotReplaceCfg �������� ���������. ������������� ��� ��� ������.
                        .addPair(ParamsEnum.peDbConnString) // todo �� ������
                        .addPair(ParamsEnum.peDbUser)
                        .addPair(ParamsEnum.peDbPwd)
        )
        msg = '����������� ������������ � ��������� ' + (retVal ? '�������' : '��') + ' ���������'
        notifyAbout(msg, OP_BIND_REPO, getNOTIFY_TYPE_AFTER(), retVal)
        retVal
    }

    def unbindRepo() {
        def msg = '������� ���������� ������������ �� ���������'
        resetResults()
        notifyAbout(msg, getOP_UNBIND_REPO(), getNOTIFY_TYPE_BEFORE())
        def retVal = execScript(
                new ExecParams(this)
                .addCommand(VanRunnerCommand.dcUnbindRepo)
                .addPair(ParamsEnum.peDbConnString) // todo �� ������
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
        )
        msg = '���������� ������������ �� ��������� ' + (retVal ? '�������' : '��') + ' ���������'
        notifyAbout(msg, getOP_UNBIND_REPO(), getNOTIFY_TYPE_AFTER(), retVal)
        retVal
    }

    // ������� ������, ���� ������ �������
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
        // � ����� �������, �� ����� ����, �������������� ��������� ������� �� ������������.
        resetResults()
        boolean retVal
        def oper = OP_UPDATE_DB
        notifyAbout('������� ���������� ���� ������', oper, NOTIFY_TYPE_BEFORE)
        ExecParams params = new ExecParams(this, VanRunnerCommand.dcUpdateDB)
            .addPair(ParamsEnum.peDbConnString)
            .addPair(ParamsEnum.peDbUser)
            .addPair(ParamsEnum.peDbPwd)
            .addPair(ParamsEnum.peUCCode, ucCode, ucCode!=null)

        retVal = execScript(params)
        notifyAbout('��������� ���������� ���� ������', oper, NOTIFY_TYPE_AFTER, retVal)
        printCmdOnce = false
        retVal
    }

//    @NonCPS
    boolean waitForCloseSessions(def maxDT, int minutesPerWaitCycle = 2, def appFilter = null){
        resetResults()
        def oper = OP_WAIT_FOR_CLOSE
        def retVal = true
        notifyAbout("������� �������� ���������� ���������. ������ '${appFilter}'; ���� �� ${maxDT.toString()} � �������� ${minutesPerWaitCycle.toString()} ���", oper, NOTIFY_TYPE_BEFORE)
        retVal = isSessionsClosed(appFilter)
        if (!retVal) {
            int sleepTime = minutesPerWaitCycle>0 ? minutesPerWaitCycle * 60 * 1000 : maxDT - Date.newInstance()
            notifyAbout("������ �������� ���������� ���������. ������ \"$appFilter\"", oper, NOTIFY_TYPE_BEFORE)
            int iter = 0
            while (Date.newInstance() < maxDT) {
                iter++
                sleep(sleepTime)
                retVal = isSessionsClosed(appFilter)
                if (retVal)
                    break
                else
                    notifyAbout("����������� �������� ���������� ���������. ����: $iter", OP_WAIT_FOR_CLOSE_CONTINUE, NOTIFY_TYPE_UNDEFINED)
            }
            if (!retVal)
                retVal = isSessionsClosed(appFilter)
        }
        notifyAbout('�������� ���������� ��������� ��������. ���������: '.concat(retVal ? '��' : '���'), oper, NOTIFY_TYPE_AFTER, retVal, maxDT, minutesPerWaitCycle, appFilter)
        retVal
    }

    boolean askDatabaseInfo(){
        resetResults()
        boolean retVal
        def oper = OP_ASK_DATABASEINFO
        notifyAbout('������� ������� ���������� � ���� ������', oper, NOTIFY_TYPE_BEFORE)
        ExecParams params = new ExecParams(this, VanRunnerCommand.dcInfo)
                .addPair(ParamsEnum.peRASServer)
                .addPair(ParamsEnum.peRACUtility)
                .addPair(ParamsEnum.peDbDatabase)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
        retVal = execScript(params)
        DatabaseInfoReader.readInfo(resultLog, databaseInfo)
        notifyAbout('���������� � ���� ������ '.concat(retVal==true ? '�������' : '��').concat(' ���������'), oper, NOTIFY_TYPE_AFTER, retVal)
        retVal
    }

    boolean unloadConfigDB(def resultFileName){
        resetResults()
        boolean retVal
        def oper = OP_UNLOAD_CONFIG_DB
        notifyAbout('������� �������� ������������ ��', oper, NOTIFY_TYPE_BEFORE)
        ExecParams params = new ExecParams(this, VanRunnerCommand.dcUnloadConfigDB)
                .addValue(resultFileName)
                .addPair(ParamsEnum.peDbConnString)
//                .addPair(ParamsEnum.peDbDatabase)
                .addPair(ParamsEnum.peDbUser)
                .addPair(ParamsEnum.peDbPwd)
        retVal = execScript(params)
        notifyAbout('�������� ������������ �� '.concat(retVal==true ? '�������' : '��').concat(' ���������'), oper, NOTIFY_TYPE_AFTER, retVal)
        retVal
    }

}