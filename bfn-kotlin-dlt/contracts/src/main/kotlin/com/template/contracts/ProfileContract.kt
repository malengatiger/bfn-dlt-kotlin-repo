package com.template.contracts

import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.template.InvoiceOfferContract
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction
import org.slf4j.LoggerFactory


class ProfileContract : Contract {
    @Throws(IllegalArgumentException::class)
    override fun verify(tx: LedgerTransaction) {
        logger.info("\uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 ProfileContract: verify starting" +
                " ..... \uD83E\uDD6C \uD83E\uDD6C ")
//        val (value, requiredSigners) = tx.getCommand<CommandData>(0)
        logger.info("Number ofCommands: ${tx.commands.size}")
//
//        if (
//                value is CreateOfferAndToken
//                || value is InvoiceOfferContract.MakeOffer
//                || value is InvoiceOfferContract.CloseOffers
//                || value is InvoiceOfferContract.InvestorSelected
//                || value is IssueTokenCommand) {
//
//            logger.info("\uD83D\uDD06 Command is of type: \uD83D\uDD06  $value \uD83D\uDD06 ")
//        } else {
//            throw IllegalArgumentException("Bad command $value")
//        }

        logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 ProfileContract: verification ( \uD83D\uDC7A none for now) done OK! " +
                ".....\uD83E\uDD1F \uD83E\uDD1F ")
    }

    class CreateProfile : CommandData
    class UpdateProfile : CommandData
    class DeleteProfile : CommandData

    companion object {
        val ID: String = ProfileContract::class.java.name
        private val logger = LoggerFactory.getLogger(ProfileContract::class.java)
    }
}
