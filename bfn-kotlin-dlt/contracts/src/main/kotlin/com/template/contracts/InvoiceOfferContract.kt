package com.template

import com.template.states.InvoiceOfferState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.crypto.toStringShort
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.utilities.toBase58String
import net.corda.core.utilities.toSHA256Bytes
import org.slf4j.LoggerFactory
import java.util.*


// ************
// * Contract *
// ************
class InvoiceOfferContract : Contract {
    @Throws(IllegalArgumentException::class)
    override fun verify(tx: LedgerTransaction) {
        logger.info("\uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 InvoiceOfferContract: verify starting" +
                " ..... \uD83E\uDD6C \uD83E\uDD6C ")
        if (tx.outputStates.size != 1) {
            throw IllegalArgumentException("One output InvoiceOfferState is required")
        }
        if (tx.commands.size != 1) {
            throw IllegalArgumentException("Only one command allowed")
        }
        val (value, requiredSigners) = tx.getCommand<CommandData>(0)
        if (value is MakeOffer || value is CloseOffers) {
            logger.info("\uD83D\uDD06 Command if of type: \uD83D\uDD06  $value \uD83D\uDD06 ")
        } else {
            throw IllegalArgumentException("Only MakeOffer or CloseOffers command allowed")
        }
        logger.info(" \uD83E\uDD8B \uD83E\uDD8B \uD83E\uDD8B  Required signers: " + requiredSigners.size)
        for (key in requiredSigners) {
            logger.info(" \uD83E\uDD8B  Required signer publicKey: ${key.toSHA256Bytes()}" +
                    " \uD83D\uDCA6 \uD83D\uDCA6 ${key.toBase58String()} \uD83D\uDCA6 \uD83D\uDCA6 ${key.toStringShort()} \uD83D\uDCA6 \uD83D\uDCA6 ")
        }
        val offerState = tx.getOutput(0) as? InvoiceOfferState
                ?: throw IllegalArgumentException("Output state must be InvoiceOfferState")

        if (offerState.offerDate.time > Date().time) {
            throw IllegalArgumentException("Offer date cannot be in the future")
        }
//        //check signatures of all parties
//        val supplierPublicKey = offerState.supplier.host.owningKey
//        logger.info(" \uD83D\uDD34 Supplier publicKey: $supplierPublicKey ☘️ Node: " + offerState.supplier.name + " - " + offerState.supplier.host.name.organisation)
//        if (!requiredSigners.contains(supplierPublicKey)) {
//            throw IllegalArgumentException("Supplier Party must sign")
//        }
//        val investorPublicKey = offerState.investor.host.owningKey
//        logger.info(" \uD83D\uDD34 Investor publicKey: $investorPublicKey ☘️ Node: " + offerState.investor.name + " - " + offerState.investor.host.name.organisation)
//        if (!requiredSigners.contains(investorPublicKey)) {
//            throw IllegalArgumentException("Investor Party must sign")
//        }
//        val customerPublicKey = offerState.customer.host.owningKey
//        logger.info(" \uD83D\uDD34 Customer publicKey: $customerPublicKey ☘️ Node: " + offerState.customer.name + " - " + offerState.customer.host.name.organisation)
//        if (!requiredSigners.contains(customerPublicKey)) {
//            throw IllegalArgumentException("Customer Party must definitely sign")
//        }
        logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 InvoiceOfferContract: verification done OK! .....\uD83E\uDD1F \uD83E\uDD1F ")
    }

    class MakeOffer : CommandData
    class CloseOffers : CommandData
    companion object {
        // This is used to identify our contract when building a transaction.
        val ID: String = InvoiceOfferContract::class.java.name
        private val logger = LoggerFactory.getLogger(InvoiceOfferContract::class.java)
    }
}
