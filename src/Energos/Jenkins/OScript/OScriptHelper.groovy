
package Energos.Jenkins.OScript;

import java.lang.*

class OScriptHelper {

    protected def script;
    protected Boolean isTestMode = false;

    Integer resultCode;
    String resultLog;
    String outputLogEncoding = 'Cp866';
    Integer interruptErrorCode = 255;
    String mainProcessName = 'oscript';
    Closure notifyClosure = null;

    public OScriptHelper(def script) {
        this.script = script;
    }

    // @NonCPS
    void selfTest(){
        echo("Включаем режим тестирования");
        isTestMode = true;
    }

    // @NonCPS
    void echo(def msg){
        if (script!=null) {
            script.echo("${msg}");
        }
    }

    void testEcho(def msg){
        if (isTestMode) 
            echo(msg);
    }
    
    void echoLog() {
        echo(resultLog);
    }

    void echoLog(String caption) {
        echo("${caption}\n${resultLog}");
    }

    void notifyAbout(def msg){
        if (notifyClosure!=null)
            notifyClosure(msg);
    }

    boolean execScript(String[] params) {

        def readLog = {InputStream st ->
            String resLog;
            resLog = new String(st.getBytes(), outputLogEncoding);
            resLog;
        }

        Boolean res;
        resultCode = null;
        resultLog = null;

        Boolean interrupted = false;

        String[] initParams = [mainProcessName];
        String[] fullParams = initParams + params;

        if (isTestMode) {
            echo("Вызов execScript в тестовом режиме с параметрами $fullParams");
            resultCode = 0;
            resultLog = 'Тестовый лог';
        res = resultCode==0;
        return res;


        } else {
            ProcessBuilder pb = new ProcessBuilder(fullParams);

            Process proc = pb.start();
            try {
                proc.waitFor();
                resultCode = proc.exitValue();
                resultLog = readLog(proc.getIn());
            } catch (InterruptedException e) {
                interrupted = true;
                resultCode = interruptErrorCode;
                resultLog = e.getMessage();
                // echo("Процесс прерван. Состояние isAlive()=${proc.isAlive()}")
                // throw (e);
            }

            if (interrupted) {
                // echo("Поток как бы прерван, ожидаем еще.");
                while (proc.isAlive()) {
                    Thread.sleep(10000);
                }
                resultCode = proc.exitValue();
                resultLog = readLog(proc.getIn());
            }
        }
    }

    boolean execScript(List<Object> params) {
        String[] strParams = new String[params.size()];
        0.upto(params.size() - 1) {
            strParams[it] = params[it].toString();    
        }
        execScript strParams;
    }

    boolean execScript(Object... args) {
        String[] strParams = new String[args.length];
        0.upto(args.length - 1) {
            strParams[it] = args[it]==null ? null : args[it].toString();
        }
        execScript strParams;
    }

    @NonCPS
    static String qStr(String value = null) {
        String retVal = value;
        if (retVal == null || retVal == '') {
            retVal = '\"\"'
        } else if (!retVal.startsWith('\"'))
            retVal = "\"$retVal\""
        return retVal;
    }

}