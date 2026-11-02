package fatum.catalog.model.vo

import fatum.catalog.model.constant.ConfirmingParty
import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class DualConfirmation(
    @Column(nullable = true)
    var byCustomer: Boolean? = null,

    @Column(nullable = true)
    var byProvider: Boolean? = null

) {
    val isConfirmed: Boolean get() = byCustomer == true && byProvider == true

    val isContradicted: Boolean get() =
        (byCustomer == true && byProvider == false) || (byCustomer == false && byProvider == true)

    val isBothDenied: Boolean get() = byCustomer == false && byProvider == false

    fun hasConfirmed(party: ConfirmingParty): Boolean = when (party) {
        ConfirmingParty.CUSTOMER -> byCustomer == true
        ConfirmingParty.PROVIDER -> byProvider == true
    }

    fun hasResponded(party: ConfirmingParty): Boolean = when (party) {
        ConfirmingParty.CUSTOMER -> byCustomer != null
        ConfirmingParty.PROVIDER -> byProvider != null
    }

    fun confirm(party: ConfirmingParty) {
        when (party) {
            ConfirmingParty.CUSTOMER -> byCustomer = true
            ConfirmingParty.PROVIDER -> byProvider = true
        }
    }

    fun deny(party: ConfirmingParty) {
        when (party) {
            ConfirmingParty.CUSTOMER -> byCustomer = false
            ConfirmingParty.PROVIDER -> byProvider = false
        }
    }
}
