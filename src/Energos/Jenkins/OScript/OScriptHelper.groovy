
package Energos.Jenkins.OScript;

class OScriptHelper {

    protected def script;

    Integer resultCode;
    String resultLog;
    String outputLogEncoding = 'Cp866';

    public OScriptHelper(def script) {
        this.script = script;
    }

    def echo(def msg){
        if (script!=null) {
            script.echo("${msg}");
        }
    }

    def execScript(String[] params) {

        def readLog = {InputStream st ->
            String resLog;
            resLog = new String(st.getBytes(), outputLogEncoding);
            resLog;
        }

        Boolean res;
        String[] initParams = ['oscript'];
        String[] fullParams = initParams + params;

        echo "$params";

        try {

            ProcessBuilder pb = new ProcessBuilder(fullParams);

            Process proc = pb.start();
            try {
                proc.waitFor();
                resultCode = proc.exitValue();
                resultLog = readLog(proc.getIn());
            } catch (e) {
                resultCode = 1;
                resultLog = e.getMessage();
            }
            res = resultCode==0;

        } finally {

        }

        res;

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
            strParams[it] = args[it].toString;
        }
        execScript(strParams);
    }

}