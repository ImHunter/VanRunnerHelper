package Energos.Jenkins.OScript

class DatabaseInfoReader {

    static void readInfo(String log, Map<String, String> mapParameters){
        mapParameters.clear()
        String[] textParameters = getTextParameters(log)
        fillMapParameters(textParameters, mapParameters)
    }

    private static String[] getTextParameters(String log){

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

        retVal.toArray()

    }

    private static void fillMapParameters(String[] textParameters, Map<String, String> mapParameters){

        def splitterPos
        def paramKey, paramValue

        textParameters.each {String ln ->
            splitterPos = ln.indexOf(':')
            if (splitterPos>0){
                paramKey = ln.substring(0, splitterPos - 1).trim()
                paramValue = ln.substring(splitterPos + 1).trim()
                mapParameters.put(paramKey, paramValue)
            }
        }

    }

}
