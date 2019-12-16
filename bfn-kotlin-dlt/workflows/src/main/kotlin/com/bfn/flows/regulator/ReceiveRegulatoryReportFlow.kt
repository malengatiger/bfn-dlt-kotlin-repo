package com.bfn.flows.regulator

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import org.slf4j.LoggerFactory

@InitiatedBy(ReportToRegulatorFlow::class)
class ReceiveRegulatoryReportFlow(private val counterPartySession: FlowSession) : FlowLogic<Void?>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): Void? {

        subFlow(ReceiveTransactionFlow(counterPartySession,
                true, StatesToRecord.ALL_VISIBLE))
        Companion.logger.info("\uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 " +
                "Regulator received state, Senor!")
        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ReceiveRegulatoryReportFlow::class.java)
    }

}
