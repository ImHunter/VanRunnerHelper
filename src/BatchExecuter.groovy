import java.util.Map
import java.io.File
import java.lang.ProcessBuilder
import java.lang.ProcessBuilder.Redirect

class BatchExecuter {

    // public vars

    // closed vars
    private Script script;
    private Map<String, String> envVariables = [:];

    // constructor
    BatchExecuter(Script scr) {
        script = scr;
    }

    ////////////////////////////////////
    // code

    def echo(def msg) {
        script.echo("${msg}");
        true;
    }

    def setEnvVariables(Map<String,String> envVars = null) {
        // envVariables = [:];
        envVariables.clear();
        if (envVars!= null) {
            envVariables.plus(envVars);
        }
        envVariables;
    }

    def execCmd(String cmdText, Map<String,String> envVars = null, Boolean returnResultAsLog = true) {

        String resLog = "";
        Integer resCode;
        def res;
        File log;

        echo cmdText;
        setEnvVariables(envVars);

        File batFile = prepareBatFile(cmdText);

        try {

            // ProcessBuilder pb = new ProcessBuilder("cmd.exe /C start /wait ${batFile.getName()}");
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/C", "START", "/WAIT", "/B", "${batFile.getName()}");
            // ProcessBuilder pb = new ProcessBuilder("cmd.exe /C START /WAIT /B \"${batFile.getName()}\"");
            // pb.environment().plus(envVariables); 
            
            File dir = new File(batFile.getParent());
            pb.directory(dir);

            log = File.createTempFile("bex",".log");
            // File log = new File("log");
            pb.redirectErrorStream(true);
            pb.redirectOutput(Redirect.appendTo(log));

            assert batFile.exists();

            Process proc = pb.start();
            proc.waitFor();
            resCode = proc.exitValue();

            // proc.getInputStream().eachLine {
            //     echo "res_line: ${it}";
            //     if (resLog==null) {
            //         resLog = it
            //     } else {
            //         resLog = resLog.concat("\n${it}")
            //     }
            // }

            resLog = log.getText();

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

    private def prepareBatFile(String cmdText) {
        File res = File.createTempFile("bex",".bat");
        res.setText(cmdText);
        // res.append("\nexit");
        res;
    }

}