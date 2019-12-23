package com.bfn.flows

import com.bfn.contractstates.contracts.ProfileContract
import com.bfn.contractstates.states.ProfileState
import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.corda.lib.accounts.workflows.ourIdentity
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import org.slf4j.LoggerFactory
import java.util.*


@InitiatingFlow
@StartableByRPC
class InvestorProfileFlow(private val profile: ProfileState) : FlowLogic<SignedTransaction>() {

    override fun call(): SignedTransaction {

        val command = ProfileContract.CreateProfile()
        val txBuilder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.first())
        val account = serviceHub.accountService.accountInfo(UUID.fromString(profile.accountId))
        txBuilder.addCommand(command, serviceHub.ourIdentity.owningKey)
        txBuilder.addOutputState(profile)
        txBuilder.verify(serviceHub)
        val tx = serviceHub.signInitialTransaction(txBuilder)
        val signedTx = subFlow(FinalityFlow(tx, listOf()))
        Companion.logger.info("\uD83E\uDD8A \uD83E\uDD8A \uD83E\uDD8A " +
                "Investor Profile has been created: ${account!!.state.data.name} \uD83E\uDD8A \uD83E\uDD8A")
        return signedTx
    }

    companion object {
        private val logger = LoggerFactory.getLogger(InvestorProfileFlow::class.java)
    }

}
