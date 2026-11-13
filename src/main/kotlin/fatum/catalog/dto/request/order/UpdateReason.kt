package fatum.catalog.dto.request.order

/**
 * Motivo opcional. No es obligatorio aquí porque cada acción decide si lo exige: negar o cancelar
 * lo exigen, confirmar solo lo aprovecha si su respuesta cierra una disputa.
 */
data class UpdateReason(
    var reason: String? = null
){
    init {
        reason = reason?.trim()
    }
}
