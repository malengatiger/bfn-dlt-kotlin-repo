package com.bfn.flows.regulator

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.ourIdentity
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import org.slf4j.LoggerFactory

@InitiatedBy(ReportToRegulatorFlow::class)
class ReceiveRegulatoryReportFlow(private val counterPartySession: FlowSession) : FlowLogic<Void?>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): Void? {
        val myself = serviceHub.ourIdentity
        val party = counterPartySession.counterparty
        logger.info("\uD83C\uDF45 \uD83C\uDF45 This party:  \uD83C\uDF45 $myself" +
                "party from session: \uD83C\uDF45  $party")

        subFlow(ReceiveTransactionFlow(counterPartySession,
                false, StatesToRecord.ALL_VISIBLE))
        Companion.logger.info("\uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 " +
                "Regulator received state, Senor!")
        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ReceiveRegulatoryReportFlow::class.java)
    }

}
