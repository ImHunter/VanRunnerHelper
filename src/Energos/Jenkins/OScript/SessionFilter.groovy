package Energos.Jenkins.OScript

class SessionFilter {

    /**
     * Возможные приложения 1С
     * 1CV8 1CV8C WebClient Designer COMConnection WSConnection BackgroundJob WebServerExtension
     */
    enum AppNames{
        /**
         * Толстый клиент
         */
        appClient {
            @NonCPS
            @Override
            String toString() { return '1CV8' }
        },
        /**
         * Тонкий клиент
         */
        appClientThin {
            @NonCPS
            @Override
            String toString() {return '1CV8C' }
        },
        /**
         * Веб-клиент
         */
        appWebClient {
            @NonCPS
            @Override
            String toString() {return 'WebClient' }
        },
        /**
         * Конфигуратор
         */
        appDesigner {
            @NonCPS
            @Override
            String toString() {return 'Designer' }
        },
        /**
         * COM-коннектор
         */
        appComConnector {
            @NonCPS
            @Override
            String toString() {return 'COMConnection' }
        },
        /**
         * Вебсервис
         */
        appWS {
            @NonCPS
            @Override
            String toString() {return 'WSConnection' }
        },
        /**
         * Фоновое задание
         */
        appBackgroung {
            @NonCPS
            @Override
            String toString() {return 'BackgroundJob' }
        },
        /**
         * Вебсервисное расширение
         */
        appWebExt {
            @NonCPS
            @Override
            String toString() {return 'WebServerExtension' }
        }
    }

    private ArrayList<String> apps = new ArrayList<>()
    private ArrayList<String> names = new ArrayList<>()

    def addAppFilter(Object... apps) {
        apps.each { this.apps.add(it.toString()) }
        this
    }

    def setNamesFilter(Object... names) {
        names.each { this.names.add(it.toString()) }
        this
    }

    @NonCPS
    private String joinArray(def array) {
        String retVal
        retVal = array.join(';')
        retVal
    }

    @NonCPS
    @Override
    java.lang.String toString() {
        java.lang.String retVal = ''
        if (apps.size!=0) {
            if (!retVal.equals('')) {
                retVal = retVal.concat('|')
            }
            retVal = retVal.concat('appid=').concat(joinArray(apps))
        }
        if (names.size!=0) {
            if (!retVal.equals('')) {
                retVal = retVal.concat('|')
            }
            retVal = retVal.concat('name=').concat(joinArray(names))
        }
        retVal
    }

    def addAppClient(){
        addAppFilter(AppNames.appClient)
    }

    def addAppClientThin(){
        addAppFilter(AppNames.appClientThin)
    }

    def addAppBackgroung(){
        addAppFilter(AppNames.appBackgroung)
    }

    def addAppComConnector(){
        addAppFilter(AppNames.appComConnector)
    }

    def addAppDesigner(){
        addAppFilter(AppNames.appDesigner)
    }

    def addAppWebClient(){
        addAppFilter(AppNames.appWebClient)
    }

    def addAppWebExt(){
        addAppFilter(AppNames.appWebExt)
    }

    def addAppWS(){
        addAppFilter(AppNames.appWS)
    }

    def addAppSome(String appName){
        addAppFilter(appName)
    }
}


