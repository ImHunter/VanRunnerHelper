
package Energos.Jenkins.OScript;

class OScriptHelper {

    protected def script;

    Integer resultCode;
    String resultLog;
    String outputLogEncoding = 'Cp866';
    Integer interruptErrorCode = 255;
    String mainProcessName = 'oscript';

    public OScriptHelper(def script) {
        this.script = script;
    }

    @NonCPS
    def echo(def msg){
        if (script!=null) {
            script.echo("${msg}");
        }
    }
    
    def echoLog() {
        echo(resultLog);
    }

    def echoLog(String caption) {
        echoLog("${caption}\n${resultLog}");
    }

    def execScript(String[] params) {

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

        ProcessBuilder pb = new ProcessBuilder(fullParams);

        Process proc = pb.start();
        try {
            proc.waitFor();
            resultCode = proc.exitValue();
            resultLog = readLog(proc.getIn());
        } catch (java.lang.InterruptedException e) {
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
        
        res = resultCode==0;

         return res;

    }

    def execScript(List<Object> params) {
        String[] strParams = new String[params.size()];
        0.upto(params.size() - 1) {
            strParams[it] = params[it].toString();    
        }
        execScript(strParams);
    }

    def execScript(Object... args) {
        String[] strParams = new String[args.length];
        0.upto(args.length - 1) {
            strParams[it] = args[it]==null ? null : args[it].toString();
        }
        execScript(strParams);
    }

    def qStr(String value = null) {
        String retVal = value;
        if (value==null || value==''){
            retVal = '\"\"'
        } else {
            if (!value.startsWith('\"'))
                retVal = "\"$value\"".toString();
        }
        return retVal;
    }

}