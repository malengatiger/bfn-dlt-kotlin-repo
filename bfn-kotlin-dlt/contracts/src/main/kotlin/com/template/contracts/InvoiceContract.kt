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
//
//        val registerCommand = tx.getCommand<Register>(0)
//
//        if (registerCommand != null) {
//            val state = tx.getOutput(0) as? InvoiceState
//                    ?: throw IllegalArgumentException("Output state must be an InvoiceState")
//            val requiredSigners = registerCommand.signers
//            checkInvoiceSignatures(state, requiredSigners)
//        }
//        val closeCommand = tx.getCommand<Close>(0)
//
//        if (closeCommand != null) {
//            val stateAndRef = tx.getInput(0) as? StateAndRef<InvoiceState>
//                    ?: throw IllegalArgumentException("Output state must be an InvoiceState")
//            val requiredSigners = closeCommand.signers
//            checkInvoiceSignatures(stateAndRef.state.data, requiredSigners)
//        }

        logger.info(" \uD83D\uDD06 \uD83D\uDD06 InvoiceContract: " +
                "\uD83C\uDF4E verification done OK! .....\uD83E\uDD1F \uD83E\uDD1F ")
    }
//
//    @Suspendable
//    fun checkInvoiceSignatures(requiredSigners: List<PublicKey>) {
//
//        val supplierPublicKey = invoiceState.supplierInfo.host.owningKey
////        logger.info(" \uD83D\uDD34 Supplier publicKey: $supplierPublicKey ☘️ Node: "
////                + invoiceState.supplierInfo.name + " - " + invoiceState.supplierInfo.host.name.organisation)
//        if (!requiredSigners.contains(supplierPublicKey)) {
//            throw IllegalArgumentException("Supplier Party must sign")
//        }
//        val customerPublicKey = invoiceState.customerInfo.host.owningKey
////        logger.info(" \uD83D\uDD34 Customer publicKey: $customerPublicKey ☘️ Node: " + invoiceState.customerInfo.name
////                + " - " + invoiceState.customerInfo.host.name.organisation)
//        if (!requiredSigners.contains(customerPublicKey)) {
//            throw IllegalArgumentException("Customer Party must sign")
//        }
//
//    }
    class Register : CommandData
    class Close : CommandData
    companion object {
        val ID: String = InvoiceContract::class.java.name
        private val logger = LoggerFactory.getLogger(InvoiceContract::class.java)
    }
}
