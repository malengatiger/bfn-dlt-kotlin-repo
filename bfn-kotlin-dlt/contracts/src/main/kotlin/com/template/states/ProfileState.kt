package com.template.states

import com.google.common.collect.ImmutableList
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.template.contracts.OfferAndTokenStateContract
import com.template.contracts.ProfileContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.math.BigDecimal
import java.util.*

@CordaSerializable
@BelongsToContract(ProfileContract::class)
class ProfileState(var issuedBy: Party,
                   val accountId: String,
                   val minimumDiscount: Double,
                   val minimumInvoiceAmount: BigDecimal,
                   val maximumInvoiceAmount: BigDecimal,
                   val maximumInvestmentPerInvoice: BigDecimal,
                   val maximumTotalInvestment: BigDecimal,
                   var date: Date
                   ) : ContractState {

    override val participants: List<AbstractParty>
        get() = ImmutableList.of<AbstractParty>(issuedBy)

}
