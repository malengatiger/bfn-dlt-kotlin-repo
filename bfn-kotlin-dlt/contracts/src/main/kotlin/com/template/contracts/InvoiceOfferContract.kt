package com.template

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.template.states.InvoiceOfferState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.crypto.toStringShort
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.utilities.toBase58String
import net.corda.core.utilities.toSHA256Bytes
import org.slf4j.LoggerFactory
import java.security.PublicKey
import java.util.*


class InvoiceOfferContract : Contract {
    @Throws(IllegalArgumentException::class)
    override fun verify(tx: LedgerTransaction) {
        logger.info("\uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 InvoiceOfferContract: verify starting" +
                " ..... \uD83E\uDD6C \uD83E\uDD6C ")
        val (value, requiredSigners) = tx.getCommand<CommandData>(0)

        logger.info("Number ofCommands: ${tx.commands.size}")

        if (value is MakeOffer || value is CloseOffers || value is InvestorSelected || value is IssueTokenCommand) {
            logger.info("\uD83D\uDD06 Command is of type: \uD83D\uDD06  $value \uD83D\uDD06 ")
        } else {
            throw IllegalArgumentException("Only MakeOffer or CloseOffers or InvestorSelected or IssueTokenCommand command allowed")
        }

        logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 InvoiceOfferContract: verification done OK! " +
                ".....\uD83E\uDD1F \uD83E\uDD1F ")
    }

    @Suspendable
    fun checkSignatures(offerState: InvoiceOfferState, requiredSigners: List<PublicKey>) {

        val supplierPublicKey = offerState.supplier.host.owningKey
        logger.info(" \uD83D\uDD34 Supplier publicKey: $supplierPublicKey ☘️ Node: "
                + offerState.supplier.name + " - " + offerState.supplier.host.name.organisation)
        if (!requiredSigners.contains(supplierPublicKey)) {
            throw IllegalArgumentException("Supplier Party must sign")
        }
        val investorPublicKey = offerState.investor.host.owningKey
        logger.info(" \uD83D\uDD34 Investor publicKey: $investorPublicKey ☘️ Node: " + offerState.investor.name + " - " + offerState.investor.host.name.organisation)
        if (!requiredSigners.contains(investorPublicKey)) {
            throw IllegalArgumentException("Investor Party must sign")
        }

    }
    class MakeOffer : CommandData
    class CloseOffers : CommandData
    class InvestorSelected: CommandData
    companion object {
        // This is used to identify our contract when building a transaction.
        val ID: String = InvoiceOfferContract::class.java.name
        private val logger = LoggerFactory.getLogger(InvoiceOfferContract::class.java)
    }
}
