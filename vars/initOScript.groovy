import Energos.Jenkins.OScript.OScriptHelper;

/**
 * Функция initOScript. Служит для получения экземпляра класса OScriptHelper.
 * @return Объект класса OScriptHelper
 */
def call(){

    new OScriptHelper(this);

}