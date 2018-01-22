
def call(Script String cmdText){
    // echo "${connector}"

    BatchExecuter executer = new BatchExecuter(this);
    executer.execCmd(cmdText);

}