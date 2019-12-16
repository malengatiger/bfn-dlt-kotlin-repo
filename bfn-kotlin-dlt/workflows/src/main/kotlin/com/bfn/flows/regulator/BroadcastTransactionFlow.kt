package com.bfn.flows.regulator

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.ourIdentity
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import org.slf4j.LoggerFactory
import java.lang.Exception

@StartableByRPC
@InitiatingFlow
class BroadcastTransactionFlow(private val signedTransaction: SignedTransaction) : FlowLogic<Void?>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): Void? {
        Companion.logger.info("\uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 " +
                "BroadcastTransactionFlow - share transaction with nodes \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 ")
        val meParty = serviceHub.ourIdentity
        val otherNodes = serviceHub.networkMapCache.allNodes

        otherNodes.forEach() {
            val party = it.legalIdentities.first()
            if (party.name.organisation == meParty.name.organisation
                    || party.name.organisation.contains("Notary")) {
                logger.info("\uD83D\uDE0E \uD83D\uDE0E ignore notary and $meParty and do not share this invoice")
            } else {
                try {
                    val counterPartySession = initiateFlow(party)
                    subFlow(SendTransactionFlow(counterPartySession, signedTransaction))
                    Companion.logger.info("\uD83E\uDD16 \uD83E\uDD16 \uD83C\uDF3F " +
                            "\uD83C\uDF3F \uD83C\uDF3F Invoice has been shared with $party !! \uD83E\uDD16 ")
                } catch (e: Exception) {
                    Companion.logger.warn("\uD83D\uDE0E \uD83D\uDE0E Unable to send invoice sharing transaction to $party")
                }
            }
        }
        Companion.logger.info("\uD83D\uDD35 \uD83D\uDD35 \uD83D\uDD35 \uD83D\uDD35 \uD83D\uDD35  " +
                "\uD83E\uDD1F Invoice sharing with  ${otherNodes.size - 2} other nodes is \uD83E\uDD1F COMPLETE \uD83E\uDD1F")

        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BroadcastTransactionFlow::class.java)
    }

}
