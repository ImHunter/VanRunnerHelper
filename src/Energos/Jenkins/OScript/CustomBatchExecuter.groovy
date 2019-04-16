package Energos.Jenkins.OScript

abstract class CustomBatchExecuter {

    // closed vars
    protected def script

    public String execLog
    public int execTimeout = 0

    // constructors
    CustomBatchExecuter() {}

    @NonCPS
    CustomBatchExecuter(def scr) {
        this.script = scr
    }

    def echo(def msg) {
        script?.echo("${msg}")
        this
    }

    def printMsg(def text){
        script?.println("$text")
        this
    }

    abstract def execute(String batchText)

}
