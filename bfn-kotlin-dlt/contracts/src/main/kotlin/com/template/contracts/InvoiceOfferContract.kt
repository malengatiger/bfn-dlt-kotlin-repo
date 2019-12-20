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

        logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 InvoiceOfferContract: verification ( \uD83D\uDC7A none at this stage :)) done OK! " +
                ".....\uD83E\uDD1F \uD83E\uDD1F ")
    }

    class MakeOffer : CommandData
    class CloseOffer : CommandData
    class InvestorSelected: CommandData
    companion object {
        // This is used to identify our contract when building a transaction.
        val ID: String = InvoiceOfferContract::class.java.name
        private val logger = LoggerFactory.getLogger(InvoiceOfferContract::class.java)
    }
}
