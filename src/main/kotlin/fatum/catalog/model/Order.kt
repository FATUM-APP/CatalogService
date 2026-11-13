package fatum.catalog.model

import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.constant.ConfirmationStage
import fatum.catalog.model.constant.ConfirmingParty
import fatum.catalog.model.constant.OrderStatus
import fatum.catalog.model.vo.DualConfirmation
import fatum.catalog.model.vo.QuestionAnswer
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "ORDERS")
class Order(

    @Column(name = "CLIENT_ID", length = 36, nullable = false)
    val clientId: String,

    @Column(name = "CLIENT_NAME", length = 255, nullable = false)
    val clientName: String,

    @Column(name = "CLIENT_USERNAME", length = 15, nullable = false)
    val clientUsername: String,

    @Column(name = "BOOKING", nullable = false)
    var booking: LocalDateTime,

    @ManyToOne
    @JoinColumn(name = "SERVICE", referencedColumnName = "ID", nullable = false)
    val catalogItem: CatalogItem,

    negotiatedPrice: BigDecimal,

    @Column(name = "DELIVERY_CITY", nullable = false, length = 50)
    val deliveryCity: String,

    @Column(name = "DELIVERY_STATE", nullable = false, length = 50)
    val deliveryState: String,

    @Column(name = "COUNTRY", nullable = false, length = 50)
    val deliveryCountry: String,

    @Column(name = "RESIDENCE", nullable = false, length = 255)
    val deliveryResidence: String,

    @Column(name = "ANSWERS", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    val answers: Set<QuestionAnswer> = emptySet(),

    @Column(name = "REQUIREMENTS", length = 500)
    val requirements: String? = null
) {
    @Column(name = "PROVIDER_ID", length = 36, nullable = false)
    val providerId: String = catalogItem.providerId

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", length = 36, nullable = false, updatable = false)
    var id: String? = null
        protected set

    @Column(name = "CREATED_AT", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "NEGOTIATED_PRICE", nullable = false, precision = 10, scale = 2)
    var negotiatedPrice: BigDecimal = BigDecimal.ZERO
        protected set(value) {
            if (value < BigDecimal.ZERO)
                throw FatumServiceException(CatalogErrorCode.NEGATIVE_PRICE)
            field = value
        }

    @Column(name = "ESTIMATED_TIME")
    var estimatedTime: Int? = catalogItem.defaultEstimatedTime
        set(value) {
            if (value != null && (value < 0 || value > 1440))
                throw FatumServiceException(CatalogErrorCode.MAX_ESTIMATED_TIME)
            field = value
        }

    @Column(name = "DELIVERY_STARTED_AT")
    var deliveryStartedAt: LocalDateTime? = null

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "byCustomer", column = Column(name = "START_CONFIRMED_BY_CUSTOMER")),
        AttributeOverride(name = "byProvider", column = Column(name = "START_CONFIRMED_BY_PROVIDER"))
    )
    private var startConfirmationValue: DualConfirmation? = DualConfirmation()

    val startConfirmation: DualConfirmation
        get() = startConfirmationValue ?: DualConfirmation().also { startConfirmationValue = it }

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "byCustomer", column = Column(name = "COMPLETION_CONFIRMED_BY_CUSTOMER")),
        AttributeOverride(name = "byProvider", column = Column(name = "COMPLETION_CONFIRMED_BY_PROVIDER"))
    )
    private var completionConfirmationValue: DualConfirmation? = DualConfirmation()

    val completionConfirmation: DualConfirmation
        get() = completionConfirmationValue ?: DualConfirmation().also { completionConfirmationValue = it }

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "byCustomer", column = Column(name = "PAYMENT_CONFIRMED_BY_CUSTOMER")),
        AttributeOverride(name = "byProvider", column = Column(name = "PAYMENT_CONFIRMED_BY_PROVIDER"))
    )
    private var paymentConfirmationValue: DualConfirmation? = DualConfirmation()

    val paymentConfirmation: DualConfirmation
        get() = paymentConfirmationValue ?: DualConfirmation().also { paymentConfirmationValue = it }

    @Column(name = "REASON", length = 500)
    var reason: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "ORDER_STATUS", nullable = false, length = 15)
    var status: OrderStatus = OrderStatus.PENDING
        protected set

    @Column(name = "UPDATED_AT", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        protected set

    init {
        this.negotiatedPrice = negotiatedPrice
    }

    fun changeStatus(newStatus: OrderStatus, reason: String? = null) {
        if (newStatus !in validTransitions[this.status].orEmpty())
            throw FatumServiceException(CatalogErrorCode.INVALID_STATUS_TRANSITION)

        val requiresReason = newStatus !in NO_REASON_REQUIRED
        if (requiresReason && reason.isNullOrBlank())
            throw FatumServiceException(CatalogErrorCode.REASON_REQUIRED)

        this.status = newStatus
        this.reason = reason
    }

    fun update(){
        this.updatedAt = LocalDateTime.now()
    }

    /** La confirmación doble de un paso, sin repetir el `when` en cada endpoint. */
    fun confirmationOf(stage: ConfirmationStage): DualConfirmation = when (stage) {
        ConfirmationStage.START -> startConfirmation
        ConfirmationStage.COMPLETION -> completionConfirmation
        ConfirmationStage.PAYMENT -> paymentConfirmation
    }

    /**
     * Registra la respuesta de una parte a un paso de confirmación y deriva el estado de la orden
     * del resultado de las DOS respuestas, no de quién respondió: niegue primero o confirme
     * primero, una confirmación y una negación siempre terminan en [OrderStatus.DISPUTED].
     *
     * Es el único punto donde una confirmación puede mover el estado, así que los seis endpoints
     * (confirmar/negar × inicio/entrega/pago) no pueden volver a divergir entre sí.
     *
     * @param reason motivo que da la parte al negar, o motivo opcional al confirmar cuando su
     * respuesta cierra una contradicción. Si llega vacío se reutiliza el motivo guardado por la
     * negación anterior y, si tampoco existe, se arma uno describiendo la contradicción.
     * @return `true` si la orden cambió de estado.
     */
    fun respondConfirmation(
        stage: ConfirmationStage,
        party: ConfirmingParty,
        confirmed: Boolean,
        reason: String? = null
    ): Boolean {
        val confirmation = confirmationOf(stage)
        if (confirmation.hasResponded(party))
            throw FatumServiceException(CatalogErrorCode.ALREADY_RESPONDED)

        if (confirmed) confirmation.confirm(party) else confirmation.deny(party)

        // La negación guarda su motivo aunque todavía no mueva el estado: si la otra parte la
        // contradice después, la disputa hereda ese motivo y no se pierde por el orden de llegada.
        // Sobrescribe siempre (también con null) para que el motivo de una etapa anterior nunca se
        // cuele como motivo de la disputa.
        val providedReason = reason.clean()
        if (!confirmed) this.reason = providedReason

        val previous = status
        resolveConfirmation(stage, confirmation, providedReason)
        return status != previous
    }

    private fun resolveConfirmation(
        stage: ConfirmationStage,
        confirmation: DualConfirmation,
        reason: String?
    ) {
        when {
            // Contradicción: da igual el paso y el orden de las respuestas.
            confirmation.isContradicted ->
                changeStatus(OrderStatus.DISPUTED, disputeReason(stage, confirmation, reason))

            // Las dos partes dicen que no se entregó: es un hecho acordado, no una disputa.
            confirmation.isBothDenied && stage == ConfirmationStage.COMPLETION ->
                changeStatus(OrderStatus.NOT_DELIVERED, reason)

            confirmation.isConfirmed && stage == ConfirmationStage.START -> {
                changeStatus(OrderStatus.DELIVERING)
                deliveryStartedAt = LocalDateTime.now()
            }

            confirmation.isConfirmed && stage == ConfirmationStage.PAYMENT ->
                changeStatus(OrderStatus.COMPLETED)
        }
    }

    /**
     * El motivo de una disputa. La negación manda: si la parte que negó escribió por qué, esa es
     * la razón de la disputa. Solo cuando nadie escribió nada se describe la contradicción.
     */
    private fun disputeReason(
        stage: ConfirmationStage,
        confirmation: DualConfirmation,
        reason: String?
    ): String {
        reason?.let { return it }
        this.reason?.clean()?.let { return it }

        val step = when (stage) {
            ConfirmationStage.START -> "el inicio"
            ConfirmationStage.COMPLETION -> "la entrega"
            ConfirmationStage.PAYMENT -> "el pago"
        }
        return if (confirmation.byCustomer == true)
            "Disputa automática: el cliente confirmó $step y el proveedor lo negó"
        else
            "Disputa automática: el cliente negó $step y el proveedor lo confirmó"
    }

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    override fun equals(other: Any?): Boolean {
        if (other !is Order) return false
        if (this === other) return true
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    companion object {
        private val NO_REASON_REQUIRED = setOf(
            OrderStatus.PENDING,
            OrderStatus.ACCEPTED,
            OrderStatus.REJECTED,
            OrderStatus.DELIVERING,
            OrderStatus.COMPLETED
        )

        private val validTransitions = mapOf(
            OrderStatus.PENDING to setOf(OrderStatus.ACCEPTED, OrderStatus.REJECTED, OrderStatus.CANCELLED),
            OrderStatus.ACCEPTED to setOf(OrderStatus.DELIVERING, OrderStatus.CANCELLED, OrderStatus.DISPUTED),
            OrderStatus.DELIVERING to setOf(OrderStatus.COMPLETED, OrderStatus.NOT_DELIVERED, OrderStatus.CANCELLED, OrderStatus.DISPUTED),
            OrderStatus.COMPLETED to setOf(OrderStatus.DISPUTED),
            OrderStatus.DISPUTED to setOf(OrderStatus.SOLVED, OrderStatus.REFUNDED)
        )
    }
}
