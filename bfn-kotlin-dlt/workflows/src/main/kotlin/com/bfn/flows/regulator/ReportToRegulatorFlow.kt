package com.bfn.flows.regulator

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import org.slf4j.LoggerFactory

@StartableByRPC
@InitiatingFlow
class ReportToRegulatorFlow(private val regulator: Party, private val signedTransaction: SignedTransaction) : FlowLogic<Void?>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): Void? {
        Companion.logger.info("\uD83D\uDE21  \uD83D\uDE21  \uD83D\uDE21  \uD83D\uDE21 " +
                "reporting to Regulator, Senor!")
        val session = initiateFlow(regulator)
        subFlow(SendTransactionFlow(session, signedTransaction))
        Companion.logger.info("\uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C" +
                "Done reporting to Regulator, Senor!")
        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ReportToRegulatorFlow::class.java)
    }

}
