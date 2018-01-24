
namespace Energos.Jenkins.OScript;

class OScriptHelper {

    protected Script script;

    Integer resultCode;
    String resultLog;
    String outputLogEncoding = 'Cp866';

    OScriptHelper(Script paramScript) {
        script = paramScript;
    }

    def echo(def msg){
        script.echo "${msg}";
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

        try {

            ProcessBuilder pb = new ProcessBuilder(fullParams);

            Process proc = pb.start();
            proc.waitFor();
            resultCode = proc.exitValue();
            resultLog = readLog(proc.getIn());
            res = execCode==0;

        } finally {
            // batFile.delete();
            // log.delete();
        }

        res;

    }

    def execScript(List<Object> params) {
        String[] strParams = new String[params.size()];
        Integer i = 0;
        params.each{ elem ->
            strParams[i] = "${elem}".toString();
            i++;
        }
        execScript(strParams);
    }

}