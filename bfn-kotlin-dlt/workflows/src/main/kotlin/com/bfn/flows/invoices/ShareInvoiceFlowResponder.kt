package com.bfn.flows.invoices

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import org.slf4j.LoggerFactory


@InitiatedBy(ShareInvoiceFlow::class)
class ShareInvoiceFlowResponder(private val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): Unit {
       Companion.logger.info("\uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C " +
               "ShareInvoiceFlowResponder starting ....")
        val myself = serviceHub.myInfo.legalIdentities.first()
        val party = counterPartySession.counterparty
        Companion.logger.info("\uD83C\uDF45 \uD83C\uDF45 This party:  \uD83C\uDF45 $myself" +
                "party from session: \uD83C\uDF45  $party")

        subFlow(ReceiveTransactionFlow(counterPartySession, true, StatesToRecord.ALL_VISIBLE))
        Companion.logger.info("\uD83D\uDC33 \uD83D\uDC33 \uD83D\uDC33 \uD83D\uDC33 \uD83D\uDC33 " +
                "ShareInvoiceFlowResponder - Transaction received .... \uD83D\uDC33")
        return
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ShareInvoiceFlowResponder::class.java)
    }

}
