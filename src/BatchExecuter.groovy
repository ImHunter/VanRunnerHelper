import java.util.Map
import java.io.File
import java.lang.ProcessBuilder
import java.lang.ProcessBuilder.Redirect
import java.lang.Runtime
import java.lang.System

class BatchExecuter {

    // public vars

    // closed vars
    private Script script;
    private Map<String, String> envVariables;

    // constructor
    BatchExecuter(Script scr) {
        script = scr;
        envVariables = System.getenv();
    }

    ////////////////////////////////////
    // code

    def echo(def msg) {
        script.echo("${msg}");
        true;
    }

    def setEnvVariables(Map<String,String> envVars = null) {
        // envVariables = [:];
        // envVariables.clear();
        if (envVars!= null) {
            envVariables.plus(envVars);
        }
        envVariables;
    }

    def execCmd(String cmdText, Map<String,String> envVars = null, Boolean returnResultAsLog = true) {

        def resLog = "";
        Integer resCode;
        def res;
        File log;

        // echo cmdText;
        setEnvVariables(envVars);

        File batFile = prepareBatFile(cmdText);

        try {

            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/C", "CALL", "${batFile.getName()}");
            File dir = new File(batFile.getParent());
            pb.directory(dir);

            Process proc = pb.start();
            proc.waitFor();
            resCode = proc.exitValue();
            resLog = proc.getText();
            // if (resCode>0) {
            //     resLog = proc.getText();
            //     // proc.getErrorStream().eachLine(){it, lnr -> 
            //     //     // echo it;
            //     //     resLog = "${resLog}\n${it}"
            //     // }
            // } else {
            //     resLog = proc.getText();
            // }

            echo "resCode=${resCode}";
            echo "resLog=${resLog}";

        } finally {
            // batFile.delete();
            // log.delete();
        }

        if (returnResultAsLog) {
            res = resLog;
        } else {
            res = resCode;
        }

        res;

    }

    def getEnvArray(Map<String, String> envMap) {
        String[] res;
        def curVal;
        if (envMap!=null) {
            def mapCount = envMap.size();
            res = new String[mapCount];
            def i = 0;
            envMap.each { entry ->
                curVal = entry.key + '=' + entry.value;
                res[i] = curVal;
                i++;
                // res.add(curVal);
            }        
        } else {
            res = [];
        }
        res;
    }

    def execCmd_run(String cmdText, Map<String, String> envVars = null, Boolean returnResultAsLog = true) {

        String resLog = "";
        Integer resCode;
        def res;
        
        setEnvVariables(envVars);
        File batFile = prepareBatFile(cmdText);

        try {

            Runtime rt = Runtime.getRuntime();
            String[] cmd = ["cmd.exe", "/A", "/C", "START", "/WAIT", "/B", batFile.getName()];
            String[] env = getEnvArray(System.getenv());
            File batDir = new File(batFile.getParent());
            Process proc = rt.exec(cmd, env, batDir);
            proc.waitFor();
            resCode = proc.exitValue();

            // ошибки
            InputStream st = proc.getErrorStream();
            st.eachLine("cp866"){it, lnr -> 
                script.println "println err: ${it} lnr ${lnr}"
            }
            st = proc.getInputStream();
            st.eachLine("cp866"){it, lnr -> 
                script.println "println info: ${it} lnr ${lnr}"
            }

        } finally {
            batFile.delete();
            // log.delete();
        }

        if (returnResultAsLog) {
            res = resLog;
        } else {
            res = resCode;
        }

        res;

    }

    private def prepareBatFile(String cmdText) {
        File res = File.createTempFile("bex",".bat");
        res.setText(cmdText);
        // res.append("\nexit");
        res;
    }

}