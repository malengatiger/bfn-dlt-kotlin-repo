package com.template.contracts

import co.paralleluniverse.fibers.Suspendable
import com.template.states.InvoiceState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.StateAndRef
import net.corda.core.transactions.LedgerTransaction
import org.slf4j.LoggerFactory
import java.security.PublicKey

class InvoiceContract : Contract {
    @Throws(IllegalArgumentException::class)
    override fun verify(tx: LedgerTransaction) {
        logger.info(
                " \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 InvoiceContract: verify starting ..... \uD83E\uDD6C \uD83E\uDD6C ")
        if (tx.outputStates.isEmpty()) {
            throw IllegalArgumentException("There should be an output state of InvoiceState")
        }
        if (tx.inputStates.isNotEmpty()) {
            throw IllegalArgumentException("There should be no input states")
        }
        val invoiceState = tx.outputStates.first() as InvoiceState
        if (invoiceState.supplierInfo.name == invoiceState.customerInfo.name) {
            throw IllegalArgumentException("\uD83D\uDC7F Supplier cannot be the same as Customer")
        }
        if (invoiceState.amount <= 0.0) {
            throw IllegalArgumentException("\uD83D\uDC7F Amount should be > zero")
        }
        if (invoiceState.totalAmount <= 0.0) {
            throw IllegalArgumentException("\uD83D\uDC7F TotalAmount should be > zero")
        }
        logger.info(" \uD83D\uDD06 \uD83D\uDD06 InvoiceContract: " +
                "\uD83C\uDF4E verification done OK! .....\uD83E\uDD1F \uD83E\uDD1F ")
    }

    class Register : CommandData
    class Close : CommandData
    companion object {
        val ID: String = InvoiceContract::class.java.name
        private val logger = LoggerFactory.getLogger(InvoiceContract::class.java)
    }
}
