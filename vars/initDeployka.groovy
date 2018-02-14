import Energos.Jenkins.OScript.DeploykaHelper

/**
 * Функция initDeployka. Служит для получения экземпляра класса DeploykaHelper
 * @param pathToDeployka Полный путь к скрипту Деплойка
 * @param pathToServiceEPF Опциональный полный путь к сервисной внешке ЗавершениеРаботы.epf. С помощью этой внешки происходит прекращение работы 1С:Предприятие после выполнения необходимых операций.
 * @return Эксемпляр DeploykaHelper
 */
def call(def pathToDeployka, def pathToServiceEPF = null){

    new DeploykaHelper(this, pathToDeployka.toString(), pathToServiceEPF==null ? null : pathToServiceEPF.toString())

}