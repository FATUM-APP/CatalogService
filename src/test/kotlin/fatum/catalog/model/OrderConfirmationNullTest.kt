package fatum.catalog.model

import fatum.catalog.model.constant.ConfirmingParty
import fatum.catalog.model.constant.JobType
import fatum.catalog.model.constant.PriceType
import fatum.catalog.model.constant.SchedulingModel
import fatum.catalog.model.vo.DualConfirmation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Hibernate devuelve `null` para un embeddable cuyas columnas están todas en NULL, aunque en Kotlin
 * el campo esté declarado como no nulo. La entidad tiene que absorber ese caso: si no, el primer
 * `confirm` revienta con un NullPointerException.
 */
class OrderConfirmationNullTest {

    private val item = CatalogItem(
        providerId = "provider-1",
        title = "Plomeria",
        providerName = "Provider",
        providerUsername = "provider",
        priceType = PriceType.FIXED,
        jobType = JobType.PRESENCIAL,
        category = Category("Hogar", SchedulingModel.SINGLE_SESSION),
        basePrice = BigDecimal(100),
        defaultEstimatedTime = 60
    )

    private fun order() = Order(
        clientId = "client-1",
        clientName = "Cliente",
        clientUsername = "cliente",
        booking = LocalDateTime.of(2026, 11, 20, 9, 0),
        catalogItem = item,
        negotiatedPrice = BigDecimal(100),
        deliveryCity = "Bogota",
        deliveryState = "Cundinamarca",
        deliveryCountry = "Colombia",
        deliveryResidence = "Calle 1",
        answers = emptySet(),
        requirements = null
    )

    /** Simula lo que entrega Hibernate cuando las seis columnas vienen en NULL. */
    private fun Order.withNull(field: String) {
        Order::class.java.getDeclaredField(field).apply {
            isAccessible = true
            set(this@withNull, null)
        }
    }

    @Test
    fun `an order read with null confirmations behaves like an empty confirmation`() {
        val order = order().apply {
            withNull("startConfirmationValue")
            withNull("completionConfirmationValue")
            withNull("paymentConfirmationValue")
        }

        assertFalse(order.startConfirmation.hasResponded(ConfirmingParty.CUSTOMER))
        assertFalse(order.startConfirmation.hasResponded(ConfirmingParty.PROVIDER))
        assertFalse(order.completionConfirmation.isConfirmed)
        assertFalse(order.paymentConfirmation.isConfirmed)
    }

    @Test
    fun `confirming after a null read keeps the answer`() {
        val order = order().apply { withNull("startConfirmationValue") }

        order.startConfirmation.confirm(ConfirmingParty.CUSTOMER)

        assertTrue(order.startConfirmation.hasResponded(ConfirmingParty.CUSTOMER))
        assertFalse(order.startConfirmation.hasResponded(ConfirmingParty.PROVIDER))
        // La misma instancia: lo que se escribió es lo que se persiste.
        assertEquals(true, order.startConfirmation.byCustomer)
    }

    @Test
    fun `a fresh order starts with nobody answering`() {
        val order = order()

        assertEquals(DualConfirmation(), order.startConfirmation)
        assertFalse(order.startConfirmation.isConfirmed)
    }
}
