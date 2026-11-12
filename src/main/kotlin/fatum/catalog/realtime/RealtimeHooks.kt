package fatum.catalog.realtime

/**
 * Puente entre los callbacks de JPA (`@PostPersist`, `@PostUpdate`, `@PostRemove`) y el
 * despachador de Spring.
 *
 * Las entidades no pueden recibir dependencias inyectadas de forma portable, y queremos que los
 * servicios sigan siendo "Kotlin plano" sin `ApplicationEventPublisher`. Cuando no hay contexto
 * de Spring (tests unitarios, entidades construidas a mano) el hook es un no-op.
 */
object RealtimeHooks {

    @Volatile
    private var dispatcher: RealtimeDispatcher? = null

    fun install(value: RealtimeDispatcher) {
        dispatcher = value
    }

    fun uninstall(value: RealtimeDispatcher) {
        if (dispatcher === value) dispatcher = null
    }

    fun current(): RealtimeDispatcher? = dispatcher
}
