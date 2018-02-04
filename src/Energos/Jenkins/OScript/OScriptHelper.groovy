
package Energos.Jenkins.OScript

import java.lang.*

/**
 * Класс предназначен для запуска скриптового файла OScript и ожидания его завершения.
 * Родился для того, чтобы уйти от использования синтаксиса bat, предоставляемого Jenkins, т.к. работа bat - неустойчива.
 * Предустановлен запуск процесса oscript. Но это можно легко поменять переприсвоением поля mainProcessName - указать любой другой вызываемый процесс (например, cmd).
 */
class OScriptHelper {

    /**
     * Переменная для хранения контекста скрипта Jenkins, чтобы можно было выполнять любые его операции.
     */
    protected def script
    /**
     * Переменная, указывающая, что действует тестовый режим. При этом процесс не запускается, а лишь в консоль выводятся параметры запуска процесса.
     */
    protected isTestMode = false

    /**
     * Код возврата после выполнения процесса.
     */
    public Integer resultCode
    /**
     * Лог, выводимый при выполнении процесса.
     */
    public String resultLog
    public String outputLogEncoding = 'Cp866'
    public Integer interruptErrorCode = 255
    public String mainProcessName = 'oscript'
    public Closure notifyClosure = null

    OScriptHelper(Script script) {
        this.script = script
    }

    // @NonCPS
    void selfTest(){
        echo("Включаем режим тестирования")
        isTestMode = true
    }

    // @NonCPS
    void echo(def msg){
        if (script!=null) {
            script.echo("${msg}")
        }
    }

    void testEcho(def msg){
        if (isTestMode) 
            echo(msg)
    }
    
    void echoLog() {
        echo(resultLog)
    }

    void echoLog(String caption) {
        echo("${caption}\n${resultLog}")
    }

    void notifyAbout(def msg){
        if (notifyClosure!=null)
            notifyClosure(msg)
    }

    boolean execScript(String[] params) {

        def readLog = {InputStream st ->
            String resLog
            resLog = new String(st.getBytes(), outputLogEncoding)
            resLog
        }

        Boolean res
        resultCode = null
        resultLog = null

        Boolean interrupted = false

        String[] initParams = [mainProcessName]
        String[] fullParams = initParams + params

        if (isTestMode) {
            echo("Вызов execScript в тестовом режиме с параметрами $fullParams")
            resultCode = 0
            resultLog = 'Тестовый лог'

        } else {
            ProcessBuilder pb = new ProcessBuilder(fullParams)

            Process proc = pb.start()
            try {
                proc.waitFor()
                resultCode = proc.exitValue()
                resultLog = readLog(proc.getIn())
            } catch (InterruptedException e) {
                interrupted = true
                resultCode = interruptErrorCode
                resultLog = e.getMessage()
            }

            if (interrupted) {
                while (proc.isAlive()) {
                    Thread.sleep(10000)
                }
                resultCode = proc.exitValue()
                resultLog = readLog(proc.getIn())
            }
        }
        res = resultCode==0
        res
    }

    boolean execScript(List<Object> params) {
        String[] strParams = new String[params.size()]
        0.upto(params.size() - 1) {
            strParams[it] = params[it].toString()
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

    @NonCPS
    static String qStr(String value = null) {
        String retVal = value
        if (retVal == null || retVal == '') {
            retVal = '\"\"'
        } else if (!retVal.startsWith('\"'))
            retVal = "\"$retVal\""
        retVal
    }

}