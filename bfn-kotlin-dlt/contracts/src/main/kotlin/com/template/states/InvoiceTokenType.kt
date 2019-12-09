package com.template.states

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.template.InvoiceTokenContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

// A token representing an invoiceOffer on ledger.
@BelongsToContract(InvoiceTokenContract::class)
data class InvoiceTokenType (
        val invoiceId: String,
        val amount: Amount<TokenType>,
        override val maintainers: List<Party>,
        override val fractionDigits: Int = 2,
        override val linearId: UniqueIdentifier
) : EvolvableTokenType()
