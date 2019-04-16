package Energos.Jenkins.OScript

class JenkinsBatchExecuter extends CustomBatchExecuter {

    def execute(String scriptText){

        def executed

        if (execTimeout > 0) {
//            try {
                script.timeout(time: execTimeout, unit: 'SECONDS') {
                    executed = executeBat(scriptText)
                }
//            }
//            catch (e){
//                execLog = 'Прервано по таймауту'
//                echo(execLog)
//                executed = false
//            }
        } else {
            executed = executeBat(scriptText)
        }

        return executed
    }

    JenkinsBatchExecuter(def scr) {
        super(scr)
    }

    private def executeBat(String scriptText){
        def executed = true
        try {
            execLog = script.bat( returnStdout: true, script: "@echo off\n".concat("chcp 65001\n").concat(scriptText))
        }
        catch (e){
            execLog = 'Не выполнено'
            executed = false
        }
        executed
    }

}
