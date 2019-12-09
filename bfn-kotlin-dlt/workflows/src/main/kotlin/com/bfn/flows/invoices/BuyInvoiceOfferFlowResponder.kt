package com.bfn.flows.invoices

import co.paralleluniverse.fibers.Suspendable
import com.bfn.flows.regulator.ReportToRegulatorFlow
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import org.slf4j.LoggerFactory
import java.util.*

@InitiatedBy(BuyInvoiceOfferFlow::class)
class BuyInvoiceOfferFlowResponder(private val counterPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
        Companion.logger.info("\uD83E\uDD6C \uD83E\uDD6C BuyInvoiceOfferFlowResponder call method at " + Date().toString())
        val serviceHub = serviceHub
        val myself = serviceHub.myInfo.legalIdentities[0]
        val party = counterPartySession.counterparty
        Companion.logger.info("\uD83C\uDF45 \uD83C\uDF45 This party: " + myself.name.toString() + ", party from session: \uD83C\uDF45 " + party.name.toString())
        Companion.logger.info("\uD83C\uDF45 \uD83C\uDF45 getCounterPartyFlowInfo: " +
                counterPartySession.getCounterpartyFlowInfo().toString())
        val signTransactionFlow: SignTransactionFlow = object : SignTransactionFlow(counterPartySession) {
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) {
            }
        }
        Companion.logger.info("\uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 run subFlow SignTransactionFlow ...")
        subFlow(signTransactionFlow)
        val signedTransaction = subFlow(ReceiveFinalityFlow(counterPartySession))
        Companion.logger.info("\uD83E\uDDE1 \uD83D\uDC9B \uD83D\uDC9A \uD83D\uDC99 \uD83D\uDC9C ReceiveFinalityFlow executed \uD83E\uDD1F")
        Companion.logger.info("\uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC4C \uD83D\uDC4C \uD83D\uDC4C Transaction finalized \uD83E\uDD1F \uD83C\uDF4F \uD83C\uDF4E $signedTransaction")
        //todo - talk to the regulator ....
        Companion.logger.info("\uD83D\uDCCC \uD83D\uDCCC \uD83D\uDCCC  Talking to the Regulator, Senor! .............")
        val parties = serviceHub.identityService.partiesFromName("Regulator", false)
        val regulator = parties.iterator().next()
        try {
            subFlow(ReportToRegulatorFlow(regulator, signedTransaction))
            Companion.logger.info("\uD83D\uDCCC \uD83D\uDCCC \uD83D\uDCCC  DONE talking to the Regulator, Phew!")
        } catch (e: Exception) {
            Companion.logger.error(" \uD83D\uDC7F  \uD83D\uDC7F  \uD83D\uDC7F Regulator fell down.  \uD83D\uDC7F IGNORED  \uD83D\uDC7F ", e)
            throw FlowException("Regulator fell down!")
        }
        return signedTransaction
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuyInvoiceOfferFlowResponder::class.java)
    }

    init {
        Companion.logger.info("BuyInvoiceOfferFlowResponder Constructor fired: \uD83C\uDF45 \uD83C\uDF45 \uD83C\uDF45")
    }
}
