// функция возвращает время с учетом попадания текущего времени в безопасный диапазон
// используется для Деплойки операция fileexists
def call(String safeHourMinFrom = '21:00', String safeHourMinTo = '07:30', Integer minutesOldForUnsafeTime = 15){

    Integer hmFrom = Integer.valueOf(safeHourMinFrom.replace(':', ''));
    Integer hmTo = Integer.valueOf(safeHourMinTo.replace(':', ''));
    def now = Calendar.getInstance();
    Integer hmNow = now.get(Calendar.HOUR_OF_DAY) * 100 + now.get(Calendar.MINUTE);
    Calendar res;

    if (hmNow<hmFrom && hmNow>hmTo) {
        now.add(Calendar.MINUTE, -minutesOldForUnsafeTime)
    } else {
        if (hmNow<=hmTo) {
            now.add(Calendar.DATE, -1);
        }
        now.clearTime()
        now.set(Calendar.HOUR_OF_DAY, hmFrom / 100);
        now.set(Calendar.MINUTE, hmFrom % 100);
    }
    now.getTime().format('yyyyMMddHHmmss');
}