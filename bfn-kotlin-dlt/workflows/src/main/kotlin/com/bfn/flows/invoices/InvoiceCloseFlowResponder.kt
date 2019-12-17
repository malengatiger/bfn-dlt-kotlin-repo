package com.bfn.flows.invoices

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.ourIdentity
import com.template.states.InvoiceOfferState
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import org.slf4j.LoggerFactory

@InitiatedBy(InvoiceCloseFlow::class)
class InvoiceCloseFlowResponder(private val counterPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
       Companion.logger.info("\uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C " +
               "InvoiceCloseFlowResponder starting ....")
        val myself = serviceHub.ourIdentity
        val party = counterPartySession.counterparty
        Companion.logger.info("\uD83C\uDF45 \uD83C\uDF45 This party: " + myself.name.toString() + ", party from session: \uD83C\uDF45 " + party.name.toString())

        val signTransactionFlow: SignTransactionFlow = object : SignTransactionFlow(counterPartySession) {
            @Suspendable
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) {
                InvoiceCloseFlowResponder.logger.info("\uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 checkTransaction here " +
                        "${stx.id} inputStates ${stx.coreTransaction.inputs.size}...")
                if (stx.coreTransaction.inputs.size != 1) {
                    throw FlowException("There must be only one input state")
                }
//                val message = stx.coreTransaction.outputStates.single() as MessageState
//                check(message.recipient == ourIdentity) { "I think you got the wrong person" }
//                check(!message.contents.containsSwearWords()) { "Mind your language" }
//                check(!message.contents.containsMemes()) { "Only serious messages are accepted" }
//                check(message.sender.name.organisation != "Nigerian Prince") { "Spam message detected" }

            }
        }
        subFlow(signTransactionFlow)
        var signedTransaction: SignedTransaction? = null
        if (myself.toString() != counterPartySession.counterparty.toString()) {
            signedTransaction = subFlow(ReceiveFinalityFlow(counterPartySession))
            if (signedTransaction != null) {
                Companion.logger.info("\uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D  InvoiceCloseFlowResponder Transaction finalized " +
                        "\uD83D\uDC4C \uD83D\uDC4C \uD83D\uDC4C \uD83E\uDD1F \uD83C\uDF4F \uD83C\uDF4E ${signedTransaction.id}")
            } else {
                Companion.logger.info(" 👿  👿  👿  👿  👿 Transaction NOT signed")
                throw IllegalStateException("Houston, we cannot sign Transaction. Not. Good")
            }
        } else {
            Companion.logger.info("\uD83D\uDD35 \uD83D\uDD35 CounterParty ${counterPartySession.counterparty} \uD83D\uDD35  " +
                    "is on the same node.  \uD83D\uDD35 Is there a need to run ReceiveFinalityFlow")
        }

        return signedTransaction!!

    }

    companion object {
        private val logger = LoggerFactory.getLogger(InvoiceCloseFlowResponder::class.java)
    }

}
