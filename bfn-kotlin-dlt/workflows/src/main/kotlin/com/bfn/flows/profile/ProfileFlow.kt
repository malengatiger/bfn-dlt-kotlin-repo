package com.bfn.flows.profile

import co.paralleluniverse.fibers.Suspendable
import com.bfn.flows.regulator.BroadcastTransactionFlow
import com.bfn.flows.regulator.ReportToRegulatorFlow
import com.google.common.collect.ImmutableList
import com.r3.corda.lib.accounts.workflows.ourIdentity
import com.template.contracts.InvoiceContract
import com.template.contracts.ProfileContract
import com.template.states.InvoiceState
import com.template.states.ProfileState
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import org.slf4j.LoggerFactory

@InitiatingFlow
@StartableByRPC
class ProfileFlow(private val profileState: ProfileState, private val action: Int = 0) : FlowLogic<SignedTransaction?>() {

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction? {
        Companion.logger.info("\uD83E\uDD1F \uD83E\uDD1F \uD83E\uDD1F \uD83E\uDD1F  " +
                "... ProfileFlow call started .. action: $action ")
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        var tx: SignedTransaction? = null
        when (action) {
            0 -> tx = createProfile(notary)
        }

        return tx;
    }

    @Suspendable
    private fun createProfile(notary: Party): SignedTransaction {
        profileState.issuedBy = serviceHub.ourIdentity
        val command = ProfileContract.CreateProfile()
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(profileState, ProfileContract.ID)
                .addCommand(
                        command, serviceHub.ourIdentity.owningKey)

        txBuilder.verify(serviceHub)
        return serviceHub.signInitialTransaction(txBuilder)
    }
    @Suspendable
    private fun updateProfile(notary: Party): SignedTransaction {
        profileState.issuedBy = serviceHub.ourIdentity
        //todo - get RefAndState by accountId and consume old, create new
//        val criteria = VaultQueryCriteria(StateStatus.UNCONSUMED)
//        serviceHub.vaultService.queryBy( criteria,
//                contractStateType = ProfileContract::class.java,
//                paging = PageSpecification(1,1000))
        val command = ProfileContract.UpdateProfile()
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(profileState, ProfileContract.ID)
                .addCommand(
                        command, serviceHub.ourIdentity.owningKey)

        txBuilder.verify(serviceHub)
        return serviceHub.signInitialTransaction(txBuilder)
    }



    companion object {
        private val logger = LoggerFactory.getLogger(ProfileFlow::class.java)

    }

}
