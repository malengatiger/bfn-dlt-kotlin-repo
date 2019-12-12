package com.template

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import com.template.states.InvoiceTokenType
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class InvoiceTokenContract


    : EvolvableTokenContract(), Contract {

    override fun additionalCreateChecks(tx: LedgerTransaction) {
        // Not much to do for this example token.
//        val newHouse = tx.outputStates.single() as InvoiceTokenType
//        newHouse.apply {
//            require(amount > Amount.zero(amount.token)) { "Amount must be greater than zero." }
//        }
    }

    override fun additionalUpdateChecks(tx: LedgerTransaction) {
//        val oldHouse = tx.inputStates.single() as InvoiceTokenType
//        val newHouse = tx.outputStates.single() as InvoiceTokenType
//        require(newHouse.amount > Amount.zero(newHouse.amount.token)) { "Valuation must be greater than zero." }
    }

    override fun verify(tx: LedgerTransaction) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
