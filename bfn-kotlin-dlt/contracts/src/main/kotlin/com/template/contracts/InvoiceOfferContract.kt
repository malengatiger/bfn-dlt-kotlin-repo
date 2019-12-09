package com.template

import com.template.states.InvoiceOfferState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction
import org.slf4j.LoggerFactory
import java.util.*

// ************
// * Contract *
// ************
class InvoiceOfferContract : Contract {
    @Throws(IllegalArgumentException::class)
    override fun verify(tx: LedgerTransaction) {
        logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 InvoiceOfferContract: verify starting" +
                " ..... \uD83E\uDD6C \uD83E\uDD6C ")
        if (tx.outputStates.size != 1) {
            throw IllegalArgumentException("One output InvoiceOfferState is required")
        }
        if (tx.commands.size != 1) {
            throw IllegalArgumentException("Only one command allowed")
        }
        val (value, requiredSigners) = tx.getCommand<CommandData>(0)
        if (value is MakeOffer || value is BuyOffer) {
        } else {
            throw IllegalArgumentException("Only MakeOffer or BuyOffer command allowed")
        }
        logger.info(" \uD83E\uDD8B \uD83E\uDD8B \uD83E\uDD8B  Required signers: " + requiredSigners.size)
        for (key in requiredSigners) {
            val sKey = Base64.getEncoder().encodeToString(key.encoded)
            logger.info(" \uD83E\uDD8B  Required signer publicKey: $sKey")
        }
        val contractState = tx.getOutput(0) as? InvoiceOfferState
                ?: throw IllegalArgumentException("Output state must be InvoiceOfferState")
        //        if (invoiceState.supplier == null) {
//            throw IllegalArgumentException("Supplier is required")
//        }
//        if (invoiceState.investor == null) {
//            throw IllegalArgumentException("Investor is required")
//        }
//        if (invoiceState.customer == null) {
//            throw IllegalArgumentException("Customer is required")
//        }
//        if (invoiceState.owner == null) {
//            throw IllegalArgumentException("Owner is definitely required")
//        }
//        if (invoiceState.offerDate == null) {
//            throw IllegalArgumentException("Offer date is required")
//        }
        if (contractState.offerDate.time > Date().time) {
            throw IllegalArgumentException("Offer date cannot be in the future")
        }
        //check signatures of all parties
        val supplierPublicKey = contractState.supplier.host.owningKey
        val sKey = Base64.getEncoder().encodeToString(supplierPublicKey.encoded)
        logger.info(" \uD83D\uDD34 Supplier publicKey: " + sKey + " ☘️ Node: " + contractState.supplier.name + " - " + contractState.supplier.host.name.organisation)
        if (!requiredSigners.contains(supplierPublicKey)) {
            throw IllegalArgumentException("Supplier Party must sign")
        }
        val investorPublicKey = contractState.investor.host.owningKey
        val iKey = Base64.getEncoder().encodeToString(investorPublicKey.encoded)
        logger.info(" \uD83D\uDD34 Investor publicKey: " + iKey + " ☘️ Node: " + contractState.investor.name + " - " + contractState.investor.host.name.organisation)
        if (!requiredSigners.contains(investorPublicKey)) {
            throw IllegalArgumentException("Investor Party must sign")
        }
        val customerPublicKey = contractState.customer.host.owningKey
        val cKey = Base64.getEncoder().encodeToString(supplierPublicKey.encoded)
        logger.info(" \uD83D\uDD34 Customer publicKey: " + cKey + " ☘️ Node: " + contractState.customer.name + " - " + contractState.customer.host.name.organisation)
        if (!requiredSigners.contains(customerPublicKey)) {
            throw IllegalArgumentException("Customer Party must definitely sign")
        }
        logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 InvoiceOfferContract: verification done OK! .....\uD83E\uDD1F \uD83E\uDD1F ")
    }

    class Register : CommandData
    class MakeOffer : CommandData
    class BuyOffer : CommandData
    companion object {
        // This is used to identify our contract when building a transaction.
        val ID = InvoiceOfferContract::class.java.name
        private val logger = LoggerFactory.getLogger(InvoiceOfferContract::class.java)
    }
}
