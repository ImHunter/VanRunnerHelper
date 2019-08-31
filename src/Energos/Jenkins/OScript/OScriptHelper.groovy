
package Energos.Jenkins.OScript

import java.lang.*


/**
 * Класс предназначен для запуска скриптового файла OScript и ожидания его завершения.
 * Родился для того, чтобы уйти от использования плагина bat, предоставляемого Jenkins, т.к. работа bat бывает неустойчива.
 * Предустановлен запуск процесса oscript. Но это можно легко поменять переприсвоением поля mainProcessName - указать любой другой вызываемый процесс (например, cmd или runner).
 */
class OScriptHelper {

    // region Защищенные поля
    /**
     * Переменная для хранения контекста скрипта Jenkins, чтобы можно было выполнять любые его операции.
     */
    protected def script
    /**
     * Переменная, указывающая, что действует тестовый режим. При этом процесс не запускается, а лишь в консоль выводятся параметры запуска процесса.
     */
    protected boolean isTestMode = false
    // endregion
    // region Публичные поля
    /**
     * Путь к выполняемому скрипту
     */
    public String pathToScript
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
    /**
     * Имя запускаемого процесса. Предустановлен запуск oscript. Имя этого процесса автоматически добавляется первым параметром при запуске скрипта.
     */
    public String mainProcessName = 'oscript'
    // endregion
    // region Приватные поля
    /**
     * Запускаемая командная строка
     */
    protected String launchString
    protected Integer currentExecTimeout = null
    protected CustomBatchExecuter executer
    // endregion

    /**
     * Конструктор класса
     * @param script В конструктор передаем контекст выполняемого скрипта Jenkins. В принципе, можно передавать null. Тогда всякие echo работать не будут. Вместо них будет использоваться println.
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
        echo("Включаем режим тестирования")
        isTestMode = true
    }

    /**
     * Вывод сообщения в какую-либо консоль. Если в объекте установлен контекст (в конструкторе передан скрипт Jenkins), то сообщение выводится его методом echo. В противном случае, печатается методом println.
     * @param msg Сообщаемое сообщение
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
     * Возврат запускаемой командной строки
     * @return Запускаемая строка
     */
    String getLaunchString(){
        launchString
    }

    /**
     * Выполнение процесса с параметрами.
     * Имя процесса содержится в поле mainProcessName и предустановление в значение oscript.
     * @param params Параметры, с которыми вызывается процесс
     * @return Возвращается Истина/true, когда процесс завершен с кодом возврата ==0. Иначе - возврат Ложь/false.
     */
    boolean execScript(String[] params) {

        def readLog = {def st ->
            String resLog
            try {
                resLog = new String(st.getBytes(), outputLogEncoding)
            } catch (e) {
                resLog = 'Ошибка чтения лога:\n'.concat(e.getMessage())
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
            echo(utf8("Вызов execScript в тестовом режиме с параметрами $fullParams"))
            resultCode = 0
            resultLog = 'Тестовый лог'

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
                        resultLog = 'Прервано по таймауту\n'.concat(readLog(proc.getIn()))
                        break
                    }
                }
                resultCode = proc.exitValue()
                resultLog = readLog(proc.getIn())
            }
        }
        setCurrentTimeout(null) // сбрасываем значение тайаута
        res = resultCode==0
        res
//        setCurrentTimeout(null)
//        return executed
    }

    /**
     * Перегруженный метод выполнения скрипта
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
     * Вспомогательный метод для обрамления двойными кавычками.
     * Если передается пустой параметр value (==null || ==''), то возвращается "".<br>
     * Если value начинается с кавычки, то дополнительное обрамление не делается.
     * @param value Параметры, с которыми вызывается процесс
     * @return Успешность выполнения скрипта (булево)
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

    @NonCPS
    def utf8(String value){
        new String(value.getBytes(), "UTF-8")
    }

    public void setCurrentTimeout(Integer secondsTimeout) {
        currentExecTimeout = secondsTimeout
        executer.execTimeout = secondsTimeout==null ? 0 : secondsTimeout
    }

}