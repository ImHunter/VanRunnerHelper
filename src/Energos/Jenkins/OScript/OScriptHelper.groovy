
package Energos.Jenkins.OScript

import java.lang.*


/**
 * ����� ������������ ��� ������� ����������� ����� OScript � �������� ��� ����������.
 * ������� ��� ����, ����� ���� �� ������������� ������� bat, ���������������� Jenkins, �.�. ������ bat ������ �����������.
 * �������������� ������ �������� oscript. �� ��� ����� ����� �������� ��������������� ���� mainProcessName - ������� ����� ������ ���������� ������� (��������, cmd ��� runner).
 */
class OScriptHelper {

    // region ���������� ����
    /**
     * ���������� ��� �������� ��������� ������� Jenkins, ����� ����� ���� ��������� ����� ��� ��������.
     */
    protected def script
    /**
     * ����������, �����������, ��� ��������� �������� �����. ��� ���� ������� �� �����������, � ���� � ������� ��������� ��������� ������� ��������.
     */
    protected boolean isTestMode = false
    // endregion
    // region ��������� ����
    /**
     * ���� � ������������ �������
     */
    public String pathToScript
    /**
     * ��� �������� ����� ���������� ��������.
     */
    public Integer resultCode
    /**
     * ���, ��������� ��� ���������� ��������.
     */
    public String resultLog
    /**
     * ���������, � ������� �������� ��� ���������� ��������. ��������������� �������� 'Cp866'
     */
    public String outputLogEncoding = 'Cp866'
    /**
     * ��� ������������ ��������. �������������� ������ oscript. ��� ����� �������� ������������� ����������� ������ ���������� ��� ������� �������.
     */
    public String mainProcessName = 'oscript'
    // endregion
    // region ��������� ����
    /**
     * ����������� ��������� ������
     */
    protected String launchString
    protected Integer currentExecTimeout = null
    protected CustomBatchExecuter executer
    // endregion

    /**
     * ����������� ������
     * @param script � ����������� �������� �������� ������������ ������� Jenkins. � ��������, ����� ���������� null. ����� ������ echo �������� �� �����. ������ ��� ����� �������������� println.
     */
    OScriptHelper(def script) {
        this.script = script
        createExecuter(script)
    }

    @NonCPS
    protected void createExecuter(def script){
        executer = new JenkinsBatchExecuter(script)
//        executer = new CmdBatchExecuter(script)
    }

    void selfTest(){
        echo("�������� ����� ������������")
        isTestMode = true
    }

    /**
     * ����� ��������� � �����-���� �������. ���� � ������� ���������� �������� (� ������������ ������� ������ Jenkins), �� ��������� ��������� ��� ������� echo. � ��������� ������, ���������� ������� println.
     * @param msg ���������� ���������
     */
    void echo(def msg){
        String echoMsg = "${msg}".toString()
        if (script!=null)
            try {
                script.echo(echoMsg)
            } catch (e) { println echoMsg }
        else
            println echoMsg
    }

    /**
     * ����� ��������� � �����-���� �������. �� ��������� ��������� � ������� ���� � �������� ������, ����� ����������� isTestMode=true
     * @param msg ���������� ���������
     */
    void testEcho(def msg){
        if (isTestMode) 
            echo(msg)
    }

    /**
     * ����� � ������� �������� ���� ���������� �������� (�� ���� resultLog).
     */
    void echoLog() {
        echo(resultLog)
    }

    /**
     * ����� � ������� �������� ���� ���������� �������� (�� ���� resultLog). �������������� ����, ����� ������� ��������� caption
     * @param caption �������������� ��������� ����
     */
    void echoLog(String caption) {
        echo("${caption}\n${resultLog}")
    }

    /**
     * ������� ����������� ��������� ������
     * @return ����������� ������
     */
    String getLaunchString(){
        launchString
    }

    /**
     * ���������� �������� � �����������.
     * ��� �������� ���������� � ���� mainProcessName � ���������������� � �������� oscript.
     * @param params ���������, � �������� ���������� �������
     * @return ������������ ������/true, ����� ������� �������� � ����� �������� ==0. ����� - ������� ����/false.
     */
    boolean execScript(String[] params) {

        def readLog = {def st ->
            String resLog
            try {
                resLog = new String(st.getBytes(), outputLogEncoding)
            } catch (e) {
                resLog = '������ ������ ����:\n'.concat(e.getMessage())
            }
            resLog
        }

        boolean res
        resetResults()

        Boolean interrupted = false
        def maxExecTime = null
        if (currentExecTimeout!=null){
            maxExecTime = Calendar.getInstance()
            maxExecTime.add(Calendar.SECOND, currentExecTimeout)
        }

        String[] initParams = [mainProcessName]
        if (pathToScript!=null && pathToScript.length()>0 && !pathToScript.equalsIgnoreCase('""'))
            initParams = initParams + [pathToScript]
        String[] fullParams = initParams + params
        launchString = fullParams.join(' ')

        def executed = true

        if (isTestMode) {
            echo("����� execScript � �������� ������ � ����������� $fullParams")
            resultCode = 0
            resultLog = '�������� ���'

        } else {
//            executed = executer.execute(fullParams)
//            resultLog = executer.execLog
//            resultCode = executed ? 0 : 1
//        }
            ProcessBuilder pb = new ProcessBuilder(fullParams)
            Process proc = pb.start()
            try {
                proc.waitFor()
                resultLog = readLog(proc.getIn())
                resultCode = proc.exitValue()
            } catch (InterruptedException e) {
                interrupted = true
                resultLog = e.getMessage()
            }

            if (interrupted) {
                while (proc.isAlive()) {
                    Thread.sleep(2500)
                    if (maxExecTime!=null && Calendar.getInstance()>maxExecTime) {
                        resultCode = null
                        resultLog = '�������� �� ��������\n'.concat(readLog(proc.getIn()))
                        break
                    }
                }
                resultCode = proc.exitValue()
                resultLog = readLog(proc.getIn())
            }
        }
        setCurrentTimeout(null) // ���������� �������� �������
        res = resultCode==0
        res
//        setCurrentTimeout(null)
//        return executed
    }

    /**
     * ������������� ����� ���������� �������
     * @param params
     * @return
     */
    boolean execScript(List<Object> params) {
        String[] strParams = new String[params.size()]
        if (params.size()>0) {
            0.upto(params.size() - 1) {
                strParams[it] = params[it].toString()
            }
        }
        execScript strParams
    }

    boolean execScript(Object... args) {
        String[] strParams = new String[args.length]
        0.upto(args.length - 1) {
            strParams[it] = args[it]==null ? null : args[it].toString()
        }
        execScript strParams
    }

    void resetResults(){
        launchString = null
        resultLog = null
        resultCode = null
    }

    /**
     * ��������������� ����� ��� ���������� �������� ���������.
     * ���� ���������� ������ �������� value (==null || ==''), �� ������������ "".<br>
     * ���� value ���������� � �������, �� �������������� ���������� �� ��������.
     * @param value ���������, � �������� ���������� �������
     * @return ���������� ���������� ������� (������)
     */
    @NonCPS
    static String qStr(String value = null) {
        String retVal = value
        if (retVal == null || retVal == '') {
            retVal = '\"\"'
        } else if (!retVal.startsWith('\"'))
            retVal = "\"$retVal\"".toString()
        retVal
    }

//    @NonCPS
//    def utf8(String value){
//        if (value!=null)
//            new String(value.getBytes(), "UTF-8")
//        else
//            value
//    }
//
//    @NonCPS
//    def ansi(String value){
//        if (value!=null)
//            new String(value.getBytes("UTF-8"), "windows-1251")
//        else
//            value
//    }

    public void setCurrentTimeout(Integer secondsTimeout) {
        currentExecTimeout = secondsTimeout
        executer.execTimeout = secondsTimeout==null ? 0 : secondsTimeout
    }

}