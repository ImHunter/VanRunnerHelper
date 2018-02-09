
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

    /**
     * Вывод сообщения в какую-либо консоль. Если в объекте установлен контекст (в конструкторе передан скрипт Jenkins), то сообщение выводится его методом echo. В противном случае, печатается методом println.
     * @param msg Сообщаемое сообщение
     */
    void echo(def msg){
        String echoMsg = "${msg}".toString()
        if (script!=null) {
            script.echo(echoMsg)
        } else
            println echoMsg
    }

    /**
     * Вывод сообщения в какую-либо консоль. Но сообщение выводится в консоль лишь в тестовом режиме, когда установлено isTestMode=true
     * @param msg Сообщаемое сообщение
     */
    void testEcho(def msg){
        if (isTestMode) 
            echo(msg)
    }

    /**
     * Вывод в консоль текущего лога выполнения процесса (из поля resultLog).
     */
    void echoLog() {
        echo(resultLog)
    }

    /**
     * Вывод в консоль текущего лога выполнения процесса (из поля resultLog). Предварительно логу, будет выведен заголовок caption
     * @param caption Дополнительный заголовок лога
     */
    void echoLog(String caption) {
        echo("${caption}\n${resultLog}")
    }

    /**
     * Метод для оповещения о каком-либо событии.
     * Вызывает выполнение notifyClosure, если эта Closure задана.
     * В notifyClosure передаются несколько параметров: notifyMsg - сообщаемое сообщение; текущий объект this.
     * @param msg Сообщаемое сообщение.
     * @param withResetResult Сбрасывать ли значения полей resultCode и resultLog в null.
     * Значение параметра true используется для оповещений ПЕРЕД выполнением операции.
     */
    void notifyAbout(def msg, boolean withResetResult = false){
        if (withResetResult) {
            resultCode = null
            resultLog = null
        }
        def notifyMsg = msg
//        if (moduleName!='')
//            notifyMsg = "$msg ($moduleName)"
        if (notifyClosure!=null)
            notifyClosure.call(notifyMsg, this)
    }

    /**
     * Выполнение процесса с параметрами.
     * Имя процесса содержится в поле mainProcessName и предустановление в значение oscript.
     * @param params Параметры, с которыми вызывается процесс
     * @return Возвращается Истина/true, когда процесс завершен с кодом возврата ==0. Иначе - возврат Ложь/false.
     */
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

    /**
     * Вспомогательный метод для обрамления двойными кавычками.
     * Если передается пустой параметр value (==null || ==''), то возвращается ""
     * Если value начинается с кавычки, то дополнительное обрамление не делается.
     * @param value
     * @return
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

}