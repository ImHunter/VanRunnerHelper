
class DeploykaHelper {

    String pathToDeployka;
    String pathToServiceEpf;
    private Script script;

    private String dbServer;
    private String dbDatabase;
    private String dbUserName;
    private String dbPassword;

    private String repoUserName;
    private String repoPassword;

    Integer execCode;
    String execLog;

    DeploykaHelper(Script paramScript, String paramPathToDeployka, String paramPathToServiceEpf) {
        pathToDeployka = paramPathToDeployka;
        pathToServiceEpf = paramPathToServiceEpf;
        script = paramScript;
    }

    def echo(def msg){
        script.echo "${msg}";
    }

    def execDeploykaCommand(String[] params) {

        def readLog = {InputStream st ->
            String res;
            st.eachLine(){ln ->
                echo "logln: ${ln}";
                if (res==null){
                    res = ln;
                } else {
                    res = "res\n${ln}".toString();
                }
            }
            res;
        }

        echo params;

        Boolean res;
        String[] initParams = ["oscript", pathToDeployka];
        String[] fullParams = initParams + params;
        echo fullParams;

        try {

            ProcessBuilder pb = new ProcessBuilder(fullParams);

            Process proc = pb.start();
            proc.waitFor();
            execCode = proc.exitValue();
            res = execCode==0;
            if (res) {
                execLog = readLog(proc.getIn());
            } else {
                execLog = readLog(proc.getErr());
            }

        } finally {
            // batFile.delete();
            // log.delete();
        }

        res;

    }

    def execDeploykaCommand(List<Object> params) {
        String[] strParams = new String[params.size()];
        Integer i = 0;
        params.each{ elem ->
            strParams[i] = "${elem}".toString();
            i++;
        }
        execDeploykaCommand(strParams);
    }

    def setDbAuth(String paramDbUserName, String paramDbPassword) {
        dbUserName = paramDbUserName;
        dbPassword = paramDbPassword;
    }

    def setRepoAuth(String paramRepoUserName, String paramRepoPassword) {
        repoUserName = paramRepoUserName;
        repoPassword = paramRepoPassword;
    }

    def setServerDatabase(String paramServer, String paramDatabase) {
        dbServer = paramServer;
        dbDatabase = paramDatabase;
    }

}