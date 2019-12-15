package com.template.states

import com.google.common.collect.ImmutableList
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.template.contracts.OfferAndTokenStateContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
@BelongsToContract(OfferAndTokenStateContract::class)
class OfferAndTokenState(val invoiceOffer: InvoiceOfferState,
                         val token: FungibleToken, private val issuer: Party) : ContractState {

    override val participants: List<AbstractParty>
        get() = ImmutableList.of<AbstractParty>(issuer)

}
