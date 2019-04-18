package Energos.Jenkins.OScript

class JenkinsBatchExecuter extends CustomBatchExecuter {

    protected def doExecute(String[] params){

        def executed

        if (execTimeout > 0) {
//            try {
                script.timeout(time: execTimeout, unit: 'SECONDS') {
                    executed = executeBat(params)
                }
//            }
//            catch (e){
//                execLog = 'Прервано по таймауту'
//                echo(execLog)
//                executed = false
//            }
        } else {
            executed = executeBat(params)
        }

        return executed
    }

    JenkinsBatchExecuter(def scr) {
        super(scr)
    }

    private def executeBat(String[] params){
        def executed = true
        def scriptText = params.join(' ')
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
