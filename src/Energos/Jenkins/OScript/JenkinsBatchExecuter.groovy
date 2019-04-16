package Energos.Jenkins.OScript

class JenkinsBatchExecuter extends CustomBatchExecuter {

    def execute(String scriptText){

        def executed

        if (execTimeout > 0) {
            try {
                script.timeout(time: execTimeout, unit: 'SECONDS') {
                    executed = executeBat(scriptText)
                }
            }
            catch (e){
                execLog = 'Прервано по таймауту'
                executed = false
            }
        } else {
            executed = executeBat(scriptText)
        }

        return executed
    }

    private def executeBat(String scriptText){
        def executed = true
        try {
            execLog = script.bat( returnStdout: true, script: 'rrr')
        }
        catch (e){
            execLog = 'Не выполнено'
            executed = false
        }
        executed
    }

}
