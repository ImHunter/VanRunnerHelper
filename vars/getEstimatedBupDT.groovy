import static java.util.Calendar.*

/**
 * Функция возвращает допустимое время с учетом попадания текущего времени в безопасный диапазон. Используется для Деплойки операция fileexists.
 * Если текущее время находится в безопасном диапазоне, то возвращается время начала безопасного диапазона.
 * Если тек. время - не в безопасном диапазоне, то возвращается время, на minutesOldForUnsafeTime минут ранее текущего.
 * @param safeHourMinFrom Начало безопасного диапазона времени. Вводится строкой формата чч:мм (HH:mm)
 * @param safeHourMinTo Окончание безопасного диапазона времени
 * @param minutesOldForUnsafeTime Количество минут для определения времени в небезопаском диапазоне. В небезопасном диапазоне считаем, что безопасны лишь последние minutesOldForUnsafeTime минут.
 * @return Минимально допустимое время при проверке бакапа. Возвращается строкой формата yyyyMMddHHmmss.
 */
def call(String safeHourMinFrom = '21:00', String safeHourMinTo = '07:30', Integer minutesOldForUnsafeTime = 15){

    Integer hmFrom = Integer.valueOf(safeHourMinFrom.replace(':', ''))
    Integer hmTo = Integer.valueOf(safeHourMinTo.replace(':', ''))
    def now = getInstance()
    def hmNow = now.get(HOUR_OF_DAY) * 100 + now.get(MINUTE)

    if (hmNow<hmFrom && hmNow>hmTo) {
        now.add(MINUTE, -minutesOldForUnsafeTime)
    } else {
        if (hmNow<=hmTo) {
            now.add(DATE, -1)
        }
        now.clearTime()
        now.set(HOUR_OF_DAY, hmFrom / 100 as int)
        now.set(MINUTE, hmFrom % 100)
    }
    now.getTime().format('yyyyMMddHHmmss')
}