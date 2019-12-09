package com.template

import com.template.states.InvoiceState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction
import org.slf4j.LoggerFactory
import java.util.*

// ************
// * Contract *
// ************
class InvoiceContract : Contract {
    @Throws(IllegalArgumentException::class)
    override fun verify(tx: LedgerTransaction) {
        logger.info(
                " \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 InvoiceContract: verify starting ..... \uD83E\uDD6C \uD83E\uDD6C ")
        if (tx.inputStates.size != 0) {
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
            val sKey = Base64.getEncoder().encodeToString(key.encoded)
            logger.info(" \uD83D\uDD34 Required signer: publicKey: $sKey")
        }
        val contractState = tx.getOutput(0) as? InvoiceState
                ?: throw IllegalArgumentException("Output state must be an InvoiceState")

        if (!requiredSigners.contains(contractState.supplierInfo.host.owningKey)) {
            throw IllegalArgumentException("Supplier Party must sign")
        }
        if (!requiredSigners.contains(contractState.customerInfo.host.owningKey)) {
            throw IllegalArgumentException("Customer Party must sign")
        }
        logger.info(" \uD83D\uDD06 \uD83D\uDD06 InvoiceContract:  \uD83C\uDF4E verification done OK! .....\uD83E\uDD1F \uD83E\uDD1F ")
    }

    class Register : CommandData
    companion object {
        // This is used to identify our contract when building a transaction.
        val ID = InvoiceContract::class.java.name
        private val logger = LoggerFactory.getLogger(InvoiceContract::class.java)
    }
}
