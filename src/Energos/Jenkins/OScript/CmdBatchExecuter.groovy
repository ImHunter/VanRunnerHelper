package Energos.Jenkins.OScript

import Energos.Jenkins.OScript.CustomBatchExecuter

class CmdBatchExecuter extends CustomBatchExecuter {

    // public vars

    // closed vars
    private Map<String, String> envVariables

    // constructor
    CmdBatchExecuter(Script scr) {
        super(scr)
        envVariables = System.getenv()
    }

    ////////////////////////////////////
    // code

    def setEnvVariables(Map<String,String> envVars = null) {
        // envVariables = [:];
        // envVariables.clear();
        if (envVars!= null) {
            envVariables + envVars
        }
        envVariables
    }

    def doExecute(String batchText){

        def readLog = {def st ->
            String resLog
            try {
                resLog = new String(st.getBytes(), 'Cp866')
            } catch (e) {
                resLog = 'Ошибка чтения лога:\n'.concat(e.getMessage())
            }
            resLog
        }

        def proc = Runtime.getRuntime().exec()
        Boolean interrupted = false
        def resultCode, res

        try {
            proc.waitFor()
            execLog = readLog(proc.getIn())
            resultCode = proc.exitValue()
        } catch (InterruptedException e) {
            interrupted = true
            execLog = e.getMessage()
        }

        def maxExecTime = null
        if (execTimeout!=null && execTimeout>0){
            maxExecTime = Calendar.getInstance()
            maxExecTime.add(Calendar.SECOND, execTimeout)
        }

        if (interrupted) {
            while (proc.isAlive()) {
                Thread.sleep(2500)
                if (maxExecTime!=null && Calendar.getInstance()>maxExecTime) {
                    resultCode = null
                    execLog = 'Прервано по таймауту\n'.concat(readLog(proc.getIn()))
                    break
                }
            }
            resultCode = proc.exitValue()
            execLog = readLog(proc.getIn())
        }
        res = resultCode==0
        res
    }

    def execCmd(String cmdText) {

        def resLog = ""
        def executed = false

        // echo cmdText;
        setEnvVariables(envVars)

        File batFile = prepareBatFile(cmdText)

        try {

            // ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/C", "CALL", "${batFile.getName()}");
            ProcessBuilder pb = new ProcessBuilder("oscript", "help")
            // echo pb.environment();
            File dir = new File(batFile.getParent())
            pb.directory(dir)

            Process proc = pb.start()
            proc.waitFor()
            executed = proc.exitValue()==0
            execLog = proc.getText()

        } finally {
            batFile.delete()
            // log.delete();
        }

        return executed

    }

    static def getEnvArray(Map<String, String> envMap) {
        String[] res
        def curVal
        if (envMap!=null) {
            def mapCount = envMap.size()
            res = new String[mapCount]
            def i = 0
            envMap.each { entry ->
                curVal = entry.key + '=' + entry.value
                res[i] = curVal
                i++
                // res.add(curVal);
            }        
        } else {
            res = []
        }
        res
    }

    def execCmd_run(String cmdText, Map<String, String> envVars = null, Boolean returnResultAsLog = true) {

        String resLog = ""
        Integer resCode
        def res

        setEnvVariables(envVars)
        File batFile = prepareBatFile(cmdText)

        try {

            Runtime rt = Runtime.getRuntime()
            String[] cmd = ["cmd.exe", "/A", "/C", "START", "/WAIT", "/B", batFile.getName()]
            String[] env = getEnvArray(System.getenv())
            File batDir = new File(batFile.getParent())
            Process proc = rt.exec(cmd, env, batDir)
            proc.waitFor()
            resCode = proc.exitValue()

            // ошибки
            InputStream st = proc.getErrorStream()
            st.eachLine("cp866"){it, lnr -> 
                script.println "println err: ${it} lnr ${lnr}"
            }
            st = proc.getInputStream()
            st.eachLine("cp866"){it, lnr -> 
                script.println "println info: ${it} lnr ${lnr}"
            }

        } finally {
            batFile.delete()
            // log.delete();
        }

        if (returnResultAsLog) {
            res = resLog
        } else {
            res = resCode
        }

        res

    }

    private static def prepareBatFile(String cmdText) {
        File res = File.createTempFile("bex",".bat")
        res.setText(cmdText)
        // res.append("\nexit");
        res
    }

}