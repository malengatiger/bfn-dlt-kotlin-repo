package com.bfn.flows.invoices

import co.paralleluniverse.fibers.Suspendable
import com.template.states.InvoiceOfferState
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import org.slf4j.LoggerFactory
/*
ERROR] 13:23:12+0200 [Node thread-1] amqp.DeserializationInput. - Serialization failed direction="Deserialize",
type="net.corda.core.transactions.SignedTransaction",
msg="Described type with descriptor net.corda:vlc3i8lJnO7K1i2g0g0+aA==
was expected to be of type class net.corda.core.transactions.SignedTransaction
but was class com.r3.corda.lib.tokens.workflows.internal.flows.finality.TransactionRole", ClassChain="net.corda.core.transactions.SignedTransaction" {fiber-id=10000117, flow-id=da0cf7f2-27f8-47d8-bb8b-8e85dbf6b22f, invocation_id=1d698164-a88f-4317-8f6b-58f516e46d37, invocation_timestamp=2019-12-14T11:23:12.675Z, origin=O=PartyC, L=Pretoria, C=ZA, session_id=1d698164-a88f-4317-8f6b-58f516e46d37, session_timestamp=2019-12-14T11:23:12.675Z, thread-id=240}
[
 */
@InitiatedBy(SelectBestInvoiceOfferFlow::class)
class SelectBestInvoiceOfferFlowResponder(private val counterPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
       Companion.logger.info("\uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C " +
               "SelectBestInvoiceOfferFlowResponder starting ....")
        val myself = serviceHub.myInfo.legalIdentities.first()
        val party = counterPartySession.counterparty
        Companion.logger.info("\uD83C\uDF45 \uD83C\uDF45 This party: " + myself.name.toString()
                + ", party from session: \uD83C\uDF45 " + party.name.toString())

        val signTransactionFlow: SignTransactionFlow = object : SignTransactionFlow(counterPartySession) {
            @Suspendable
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) {
                SelectBestInvoiceOfferFlowResponder.logger.info("\uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 checkTransaction here " +
                        "${stx.id} outputStates ${stx.coreTransaction.outputStates.size} ...")
            }
        }
        Companion.logger.info("\uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 run subFlow SignTransactionFlow ...")
        subFlow(signTransactionFlow)
        var signedTransaction: SignedTransaction? =  null
        if (myself.toString() != counterPartySession.counterparty.name.toString()) {
            signedTransaction = subFlow(ReceiveFinalityFlow(counterPartySession))
        }
        if (signedTransaction != null) {
            Companion.logger.info("\uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D  SelectBestInvoiceOfferFlowResponder Transaction finalized " +
                    "\uD83D\uDC4C \uD83D\uDC4C \uD83D\uDC4C \uD83E\uDD1F \uD83C\uDF4F \uD83C\uDF4E ${signedTransaction.id}")
        }

        return signedTransaction!!
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SelectBestInvoiceOfferFlowResponder::class.java)
    }

}
