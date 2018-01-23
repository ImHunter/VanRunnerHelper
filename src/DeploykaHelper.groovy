
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

    def execDeploykaCommand(String[] params) {

        Boolean res;
        String[] initParams = ["oscript", pathToDeployka];
        def fullParams = initParams + params;

        try {

            ProcessBuilder pb = new ProcessBuilder(fullParams);

            Process proc = pb.start();
            proc.waitFor();
            execCode = proc.exitValue();
            execLog = proc.getText();

        } finally {
            // batFile.delete();
            // log.delete();
        }

        res = execCode==0;
        res;

    }

    def execDeploykaCommand(List params) {
        String[] strParams = new String[params.size()];
        def i = 0;
        params.forEach{
            strParams[i] = it.toString();
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