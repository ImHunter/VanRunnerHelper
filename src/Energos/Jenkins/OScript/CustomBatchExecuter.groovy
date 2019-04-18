package Energos.Jenkins.OScript

abstract class CustomBatchExecuter {

    // closed vars
    protected def script

    public String execLog
    public int execTimeout = 0

    // constructors
    CustomBatchExecuter() {}

    CustomBatchExecuter(def scr) {
        this.script = scr
    }

    def echo(def msg) {
        script?.echo("${msg}")
        this
    }

    def execute(String[] params){
        def executed = doExecute(params)
        execTimeout = 0
        executed
    }

    def printMsg(def text){
        script?.println("$text")
        this
    }

    abstract protected def doExecute(String[] params)

}
