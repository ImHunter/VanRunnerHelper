
class DeploykaHelper {

    private String pathToDeployka;
    private String pathToServiceEpf;
    private Script script;

    private String dbUserName;
    private String dbPassword;

    private String repoUserName;
    private String repoPassword;

    DeploykaHelper(Script paramScript, String paramPathToDeployka, String paramPathToServiceEpf) {
        pathToDeployka = paramPathToDeployka;
        pathToServiceEpf = paramPathToServiceEpf;
        script = paramScript;
    }

    def execDeploykaCommand() {
        
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