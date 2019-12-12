package com.template.states

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.template.InvoiceTokenContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party

// A token representing an invoiceOffer on ledger.
@BelongsToContract(InvoiceTokenContract::class)
data class InvoiceTokenType(
        val invoice: InvoiceState,
        val issuer: Party,
        val holder: AnonymousParty,
        override val tokenIdentifier: String,
        override val fractionDigits: Int = 2

) : TokenType(tokenIdentifier, fractionDigits)
