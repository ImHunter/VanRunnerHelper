
def call(String cmdText, Boolean returnResultAsLog = true){
    // echo "${connector}"

    BatchExecuter executer = new BatchExecuter(this);
    executer.execCmd(cmdText, null, returnResultAsLog);

}