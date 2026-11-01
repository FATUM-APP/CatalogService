package fatum.catalog.model

import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.constant.ConfirmingParty
import fatum.catalog.model.constant.OrderStatus
import fatum.catalog.model.vo.DualConfirmation
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
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "ORDERS")
class Order(

    @Column(name = "CLIENT_ID", length = 36, nullable = false)
    val clientId: String,

    @Column(name = "CLIENT_USERNAME", length = 15, nullable = false)
    val clientUsername: String,

    @Column(name = "BOOKING", nullable = false)
    var booking: LocalDateTime,

    @ManyToOne
    @JoinColumn(name = "SERVICE", referencedColumnName = "ID", nullable = false)
    val catalogItem: CatalogItem,

    negotiatedPrice: BigDecimal = catalogItem.basePrice,

    estimatedTime: Int? = catalogItem.defaultEstimatedTime,

    @Column(name = "DELIVERY_CITY", nullable = false, length = 50)
    val deliveryCity: String,

    @Column(name = "DELIVERY_STATE", nullable = false, length = 50)
    val deliveryState: String,

    @Column(name = "COUNTRY", nullable = false, length = 50)
    val deliveryCountry: String,

    @Column(name = "RESIDENCE", nullable = false, length = 255)
    val deliveryResidence: String,
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
        set(value) {
            if (value < BigDecimal.ZERO)
                throw FatumServiceException(CatalogErrorCode.NEGATIVE_PRICE)
            field = value
        }

    @Column(name = "ESTIMATED_TIME")
    var estimatedTime: Int? = null
    set(value) {
        if (value != null && (value < 0 || value > 1440))
            throw FatumServiceException(CatalogErrorCode.MAX_ESTIMATED_TIME)
        field = value
    }

    @Column(name = "DELIVERY_STARTED_AT")
    var deliveryStartedAt: LocalDateTime? = null
        protected set

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "byCustomer", column = Column(name = "START_CONFIRMED_BY_CUSTOMER")),
        AttributeOverride(name = "byProvider", column = Column(name = "START_CONFIRMED_BY_PROVIDER"))
    )
    val startConfirmation: DualConfirmation = DualConfirmation()

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "byCustomer", column = Column(name = "COMPLETION_CONFIRMED_BY_CUSTOMER")),
        AttributeOverride(name = "byProvider", column = Column(name = "COMPLETION_CONFIRMED_BY_PROVIDER"))
    )
    val completionConfirmation: DualConfirmation = DualConfirmation()

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "byCustomer", column = Column(name = "PAYMENT_CONFIRMED_BY_CUSTOMER")),
        AttributeOverride(name = "byProvider", column = Column(name = "PAYMENT_CONFIRMED_BY_PROVIDER"))
    )
    val paymentConfirmation: DualConfirmation = DualConfirmation()

    @Column(name = "REASON", length = 500)
    var reason: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "ORDER_STATUS", nullable = false, length = 12)
    var status: OrderStatus = OrderStatus.PENDING


    init {
        this.negotiatedPrice = negotiatedPrice
        this.estimatedTime = estimatedTime
        this.catalogItem.addOrder(this)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Order) return false
        if (this === other) return true
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

}

