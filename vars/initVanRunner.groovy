import Energos.Jenkins.OScript.VanRunnerHelper

/**
 * Функция initVanRunner. Служит для получения экземпляра класса DeploykaHelper
 * @param pathToScript Полный путь к скрипту Деплойка
 * @param pathToServiceEPF Опциональный полный путь к сервисной внешке ЗавершениеРаботы.epf. С помощью этой внешки происходит прекращение работы 1С:Предприятие после выполнения необходимых операций.
 * @return Эксемпляр VanRunnerHelper
 */
def call(def pathToScript = null, def pathToServiceEPF = null){

    new VanRunnerHelper(this, pathToScript?.toString(), pathToServiceEPF?.toString())

}
