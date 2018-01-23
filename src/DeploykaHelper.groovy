
class DeploykaHelper {

    private String pathToDeployka;
    private String pathToServiceEpf;
    private Script script;

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

        try {

            ProcessBuilder pb = new ProcessBuilder(initParams + params);

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

    def setDbAuth(String paramDbUserName, String paramDbPassword) {
        dbUserName = paramDbUserName;
        dbPassword = paramDbPassword;
    }

    def setRepoAuth(String paramRepoUserName, String paramRepoPassword) {
        repoUserName = paramRepoUserName;
        repoPassword = paramRepoPassword;
    }

}