package com.bfn.flows.scheduled

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.ourIdentity
import com.template.states.InvoiceOfferState
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import org.slf4j.LoggerFactory


@InitiatedBy(MakeInvoiceOffersFlow::class)
class MakeInvoiceOffersFlowResponder(
        private val counterPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
       Companion.logger.info("\uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C " +
               "MakeInvoiceOffersFlowResponder starting ....")
        val myself = serviceHub.ourIdentity
        val party = counterPartySession.counterparty
        Companion.logger.info("\uD83C\uDF45 \uD83C\uDF45 This party: " + myself.name.toString() + ", party from session: \uD83C\uDF45 " + party.name.toString())

        val signTransactionFlow: SignTransactionFlow = object : SignTransactionFlow(counterPartySession) {
            @Suspendable
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) {
                MakeInvoiceOffersFlowResponder.logger.info("\uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 checkTransaction here " +
                        "${stx.id} outputStates ${stx.coreTransaction.outputStates.size}...")

            }
        }
        subFlow(signTransactionFlow)
        val signedTransaction = subFlow(ReceiveFinalityFlow(counterPartySession))
        Companion.logger.info("\uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D  MakeInvoiceOffersFlowResponder Transaction finalized " +
                "\uD83D\uDC4C \uD83D\uDC4C \uD83D\uDC4C \uD83E\uDD1F \uD83C\uDF4F \uD83C\uDF4E ${signedTransaction.id}")

        return signedTransaction
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MakeInvoiceOffersFlowResponder::class.java)
    }

}
