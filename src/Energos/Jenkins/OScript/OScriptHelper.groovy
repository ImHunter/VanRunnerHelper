
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
    protected Script script
    /**
     * Переменная, указывающая, что действует тестовый режим. При этом процесс не запускается, а лишь в консоль выводятся параметры запуска процесса.
     */
    protected boolean isTestMode = false

    /**
     * Код возврата после выполнения процесса.
     */
    public Integer resultCode
    /**
     * Лог, выводимый при выполнении процесса.
     */
    public String resultLog
    /**
     * Кодировка, в которой читается лог выполнения процесса. Предустановлено значение 'Cp866'
     */
    public String outputLogEncoding = 'Cp866'
//    public Integer interruptErrorCode = 255
    /**
     * Имя запускаемого процесса. Предустановлен запуск oscript. Имя этого процесса автоматически добавляется первым параметром при запуске скрипта.
     */
    public String mainProcessName = 'oscript'
    /**
     * Closure, которая может быть использована для логирования операций. Вызывается внутри метода notifyAbout
     */
    public Closure notifyClosure = null
    /**
     * Опциональное имя объекта. Используется для логирования.
     */
    public String moduleName = ''

    /**
     * Конструктор класса
     * @param script В конструктор передаем контекст выполняемого скрипта Jenkins. В принципе, можно передавать null. Тогда всякие echo работать не будут. Вместо них будет использоваться println.
     */
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
        String echoMsg = "${msg}".toString()
        if (script!=null) {
            script.echo(echoMsg)
        } else
            println echoMsg
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

    String notifyAbout(def msg){
        def notifyMsg = msg;
        if (moduleName!='')
            notifyMsg = "$msg ($moduleName)"
        if (notifyClosure!=null)
            notifyClosure(notifyMsg)
        notifyMsg
    }

    boolean execScript(String[] params) {

        def readLog = {InputStream st ->
            String resLog
            resLog = new String(st.getBytes(), outputLogEncoding)
            resLog
        }

        boolean res
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
//                resultCode = interruptErrorCode
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
            retVal = "\"$retVal\"".toString()
        retVal
    }

}