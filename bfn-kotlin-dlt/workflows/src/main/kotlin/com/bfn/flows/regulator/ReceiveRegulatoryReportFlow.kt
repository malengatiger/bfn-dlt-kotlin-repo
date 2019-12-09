package com.bfn.flows.regulator

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import org.slf4j.LoggerFactory

@InitiatedBy(ReportToRegulatorFlow::class)
class ReceiveRegulatoryReportFlow(val otherSession: FlowSession) : FlowLogic<Void?>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): Void? {
        Companion.logger.info("\uD83D\uDE21  \uD83D\uDE21  \uD83D\uDE21  \uD83D\uDE21 " +
                "Regulator receiving, Senor!")
        subFlow(ReceiveTransactionFlow(otherSession,
                true, StatesToRecord.ALL_VISIBLE))
        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ReceiveRegulatoryReportFlow::class.java)
    }

}
