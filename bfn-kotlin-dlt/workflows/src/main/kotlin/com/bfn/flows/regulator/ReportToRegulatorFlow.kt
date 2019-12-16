package com.bfn.flows.regulator

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import org.slf4j.LoggerFactory

@StartableByRPC
@InitiatingFlow
class ReportToRegulatorFlow(private val signedTransaction: SignedTransaction) : FlowLogic<Void?>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): Void? {
        Companion.logger.info("\uD83D\uDE21  \uD83D\uDE21  \uD83D\uDE21  \uD83D\uDE21 " +
                "reporting to Regulator, Senor!")
        //todo - get regulator details from address
        val regulatorNode = serviceHub.networkMapCache.getNodeByLegalName(
                CordaX500Name(organisation = "Regulator", country = "ZA", locality = "Pretoria"))

        val regulatorParty = regulatorNode?.legalIdentities?.first()
        val session = regulatorParty?.let { initiateFlow(it) }
        session?.let { SendTransactionFlow(it, signedTransaction) }?.let { subFlow(it) }
        Companion.logger.info("\uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C" +
                "Done reporting to Regulator, Senor!")
        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ReportToRegulatorFlow::class.java)
    }

}
