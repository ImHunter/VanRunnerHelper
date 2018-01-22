import java.util.Map
import java.io.File
import java.lang.ProcessBuilder

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

        String[] resLog = [];
        Integer resCode = 0;
        def res;

        setEnvVariables(envVars);

        File batFile = prepareBatFile(cmdText);
        try {
            // ProcessBuilder pb = new ProcessBuilder("cmd.exe /C start /wait ${batFile.getName()}");
            ProcessBuilder pb = new ProcessBuilder(["cmd.exe", "/C", "start", "/wait", "${batFile.getName()}"]);
            pb.environment().plus(envVariables); 
            File dir = new File(batFile.getParent());
            pb.directory(dir);
            Process proc = pb.start();
            resCode = proc.waitfor();
        } finally {
            // batFile.delete();
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
        res;
    }

}