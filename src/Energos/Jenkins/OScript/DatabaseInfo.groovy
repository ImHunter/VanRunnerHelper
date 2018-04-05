package Energos.Jenkins.OScript

class DatabaseInfo {

    Map<String, String> parameters = [:]

    void readInfo(String log){
        String textParameters = getTextParameters(log)
        fillMapParameters(textParameters)
    }

    private String getTextParameters(String log){

        def ArrayList<String> retVal = new ArrayList<>()

        boolean markDetected = false

        Scanner scanner = new Scanner(log)
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine()
            if (!markDetected) {
                if (line.toUpperCase().contains('ИНФОРМАЦИЯ - Получен УИД базы'.toUpperCase())) {
                    markDetected = true
                    scanner.nextLine()
                }
            } else
                retVal.add(line)
        }
        scanner.close()
    }

    private void fillMapParameters(String[] textParameters){

    }

}
