package com.bfn.flows.invoices

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.ourIdentity
import com.template.states.InvoiceOfferState
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import org.slf4j.LoggerFactory

@InitiatedBy(SelectBestInvoiceOfferFlow::class)
class SelectBestInvoiceOfferFlowResponder(private val counterPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
       Companion.logger.info("\uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C " +
               "SelectBestInvoiceOfferFlowResponder starting ....")
        val myself = serviceHub.ourIdentity
        val party = counterPartySession.counterparty

        Companion.logger.info("\uD83C\uDF45 \uD83C\uDF45 SelectBestInvoiceOfferFlowResponder: " +
                "This party: \uD83C\uDF4E $myself \uD83C\uDF45 \uD83C\uDF45 counterParty: $party" )

        val signTransactionFlow: SignTransactionFlow = object : SignTransactionFlow(counterPartySession) {
            @Suspendable
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) {
                SelectBestInvoiceOfferFlowResponder.logger.info("\uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 would be checking Transaction here " +
                        "${stx.id} outputStates ${stx.coreTransaction.outputStates.size} ...")
                stx.coreTransaction.outputStates.forEach() {
                    SelectBestInvoiceOfferFlowResponder.logger.info("Output State: ${it.participants}")
                }
            }
        }

        Companion.logger.info("\uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 about to run subFlow SignTransactionFlow ..." +
                " \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06")
        subFlow(signTransactionFlow)
        Companion.logger.info("\uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 subFlow SignTransactionFlow completed..." +
                " \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06")

        var signedTransaction: SignedTransaction? =  null
        if (myself.toString() != party.toString()) {
            signedTransaction = subFlow(ReceiveFinalityFlow(counterPartySession))
        } else {
            Companion.logger.info("\uD83C\uDF36 \uD83C\uDF36  SelectBestInvoiceOfferFlowResponder - \uD83D\uDC7F " +
                    "HAVE NOT signedTransaction: \uD83C\uDF36 \uD83C\uDF36 counterParty is the same as Party. is this cool?")
        }
        if (signedTransaction != null) {
            Companion.logger.info("\uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D  " +
                    "SelectBestInvoiceOfferFlowResponder Transaction finalized " +
                    "\uD83D\uDC4C \uD83D\uDC4C \uD83D\uDC4C \uD83E\uDD1F \uD83C\uDF4F \uD83C\uDF4E ${signedTransaction.id}")
        } else {
            Companion.logger.info("\uD83D\uDC7F \uD83D\uDC7F SelectBestInvoiceOfferFlowResponder - " +
                    "\uD83D\uDC7F signedTransaction is NULL")
        }

        return signedTransaction!!
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SelectBestInvoiceOfferFlowResponder::class.java)
    }

}
