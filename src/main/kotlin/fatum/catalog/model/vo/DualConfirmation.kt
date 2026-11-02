package fatum.catalog.model.vo

import fatum.catalog.model.constant.ConfirmingParty
import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class DualConfirmation(
    @Column(nullable = false)
    var byCustomer: Boolean = false,

    @Column(nullable = false)
    var byProvider: Boolean = false
) {
    val isConfirmed: Boolean get() = byCustomer && byProvider

    fun hasConfirmed(party: ConfirmingParty): Boolean = when (party) {
        ConfirmingParty.CUSTOMER -> byCustomer
        ConfirmingParty.PROVIDER -> byProvider
    }

    fun confirm(party: ConfirmingParty) {
        when (party) {
            ConfirmingParty.CUSTOMER -> byCustomer = true
            ConfirmingParty.PROVIDER -> byProvider = true
        }
    }
}