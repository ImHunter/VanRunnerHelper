package Energos.Jenkins.OScript

abstract class CustomBatchExecuter {

    // closed vars
    protected Script script

    public String execLog
    public int execTimeout = 0

    // constructors
    CustomBatchExecuter() {}

    CustomBatchExecuter(Script scr) {
        script = scr
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
