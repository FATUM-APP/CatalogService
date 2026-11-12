package fatum.catalog.realtime

/**
 * Destinos STOMP del servicio.
 *
 * Convención:
 *  - `/topic/...`  : canales públicos de solo lectura (calendario de un proveedor).
 *  - `/user/queue/...` : canales privados por usuario. Spring traduce el destino a una cola
 *    exclusiva de la sesión del usuario autenticado, por lo que nadie más puede escucharla.
 */
object RealtimeTopics {

    const val APP_PREFIX = "/app"
    const val USER_PREFIX = "/user"

    /** Calendario público de un proveedor: fechas, horarios y ocupación (sin datos personales). */
    fun publicCalendar(providerId: String): String = "/topic/calendar/$providerId"

    /** Bandeja de órdenes del usuario autenticado (cliente o proveedor). */
    const val USER_ORDERS = "/queue/orders"

    /** Calendario privado del proveedor autenticado (incluye ids de orden). */
    const val USER_CALENDAR = "/queue/calendar"

    const val PUBLIC_CALENDAR_PATTERN = "/topic/calendar/*"
}
