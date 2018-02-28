package Energos.Jenkins.OScript

/**
 * Класс для отражения информации о конфигурации.
 * Заполняется по итогам запуска сервисной обработки в режиме 1С:Предприятие. Обработка выводит определенные логи. Их содержимое интерпретируется и отражается в полях объекта.
 */
class ConfigInfo {

    Boolean isChanged
    String shortName
    String version
    String platform

    private void readLogInfo(String log) {

        String paramValue

        isChanged = null
        paramValue = readParamValue(log, 'CONFIG_STATE')
        // echo "value of CONFIG_STATE: ${paramValue}";
        if (paramValue!=null) {
            if (paramValue.toUpperCase() == 'CONFIG_CHANGED') {
                isChanged = true
            } else {
                if (paramValue.toUpperCase() == 'CONFIG_NOT_CHANGED') {
                    isChanged = false
                }
            }
        }
        shortName = readParamValue(log, 'SHORT_CONFIG_NAME')
        version = readParamValue(log, 'CONFIG_VERSION')
        platform = readParamValue(log, 'PLATFORM')
    }

    private String readParamValue(String log, String paramName) {
        String retVal = null
        Scanner scanner = new Scanner(log)
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine()
            Integer posParam = line.toUpperCase().indexOf(paramName.toUpperCase())
            if (posParam>=0) {
                retVal = line.substring(posParam + paramName.length())
                if (retVal.startsWith(':')){
                    retVal = retVal.substring(1)
                }
                break
            }
        }
        scanner.close()
        retVal
    }

}

