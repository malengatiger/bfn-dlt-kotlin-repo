package com.bfn.flows.invoices

import co.paralleluniverse.fibers.Suspendable
import com.bfn.flows.invoices.InvoiceRegistrationFlow
import com.bfn.flows.regulator.ReportToRegulatorFlow
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import org.slf4j.LoggerFactory

@InitiatedBy(InvoiceRegistrationFlow::class)
class InvoiceRegistrationFlowResponder(private val counterPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
        Companion.logger.info("\uD83E\uDD6C \uD83E\uDD6C InvoiceRegistrationFlowResponder call method at ")
        val serviceHub = serviceHub
        val myself = serviceHub.myInfo.legalIdentities[0]
        val party = counterPartySession.counterparty
        Companion.logger.info("\uD83C\uDF45 \uD83C\uDF45 This party: " + myself.name.toString() + ", party from session: \uD83C\uDF45 " + party.name.toString())
        Companion.logger.info("\uD83C\uDF45 \uD83C\uDF45 getCounterPartyFlowInfo: " +
                counterPartySession.getCounterpartyFlowInfo().toString())

        val signTransactionFlow: SignTransactionFlow = object : SignTransactionFlow(counterPartySession) {
            @Suspendable
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) {
            }
        }
        subFlow(signTransactionFlow)
        val signedTransaction = subFlow(ReceiveFinalityFlow(counterPartySession))
        Companion.logger.info("\uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D  InvoiceRegistrationFlowResponder finalized!" +
                " \uD83D\uDC4C \uD83D\uDC4C \uD83D\uDC4C \uD83E\uDD1F \uD83C\uDF4F \uD83C\uDF4E $signedTransaction")
        try {
            subFlow(ReportToRegulatorFlow(signedTransaction))
        } catch (e: Exception) {
            Companion.logger.error(" \uD83D\uDC7F  \uD83D\uDC7F  \uD83D\uDC7F Regulator fell down.  \uD83D\uDC7F IGNORED  \uD83D\uDC7F ", e)
            throw FlowException("Regulator fell down!")
        }
        return signedTransaction
    }

    companion object {
        private val logger = LoggerFactory.getLogger(InvoiceRegistrationFlowResponder::class.java)
    }

}
