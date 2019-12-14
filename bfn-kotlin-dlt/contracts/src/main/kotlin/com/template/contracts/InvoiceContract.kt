package com.template

import co.paralleluniverse.fibers.Suspendable
import com.template.states.InvoiceState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction
import org.slf4j.LoggerFactory
import java.security.PublicKey

class InvoiceContract : Contract {
    @Throws(IllegalArgumentException::class)
    override fun verify(tx: LedgerTransaction) {
        logger.info(
                " \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 InvoiceContract: verify starting ..... \uD83E\uDD6C \uD83E\uDD6C ")
        if (tx.inputStates.isNotEmpty()) {
            throw IllegalArgumentException("Input states must be zero")
        }
        if (tx.outputStates.size != 1) {
            throw IllegalArgumentException("One output InvoiceState is required")
        }
        if (tx.commands.size != 1) {
            throw IllegalArgumentException("Only one command allowed")
        }
        val (value, requiredSigners) = tx.getCommand<Register>(0)

        logger.info(" \uD83D\uDD34  \uD83D\uDD34 Required signers: " + requiredSigners.size)
        for (key in requiredSigners) {
            logger.info(" \uD83D\uDD34 Required signer: publicKey: $key")
        }
        tx.getOutput(0) as? InvoiceState
                ?: throw IllegalArgumentException("Output state must be an InvoiceState")

        checkSignatures(tx.outputStates.first() as InvoiceState, requiredSigners)

        logger.info(" \uD83D\uDD06 \uD83D\uDD06 InvoiceContract: " +
                "\uD83C\uDF4E verification done OK! .....\uD83E\uDD1F \uD83E\uDD1F ")
    }
    @Suspendable
    fun checkSignatures(offerState: InvoiceState, requiredSigners: List<PublicKey>) {

        val supplierPublicKey = offerState.supplierInfo.host.owningKey
        logger.info(" \uD83D\uDD34 Supplier publicKey: $supplierPublicKey ☘️ Node: "
                + offerState.supplierInfo.name + " - " + offerState.supplierInfo.host.name.organisation)
        if (!requiredSigners.contains(supplierPublicKey)) {
            throw IllegalArgumentException("Supplier Party must sign")
        }
        val customerPublicKey = offerState.customerInfo.host.owningKey
        logger.info(" \uD83D\uDD34 Customer publicKey: $customerPublicKey ☘️ Node: " + offerState.customerInfo.name
                + " - " + offerState.customerInfo.host.name.organisation)
        if (!requiredSigners.contains(customerPublicKey)) {
            throw IllegalArgumentException("Customer Party must sign")
        }

    }
    class Register : CommandData
    companion object {
        val ID: String = InvoiceContract::class.java.name
        private val logger = LoggerFactory.getLogger(InvoiceContract::class.java)
    }
}
