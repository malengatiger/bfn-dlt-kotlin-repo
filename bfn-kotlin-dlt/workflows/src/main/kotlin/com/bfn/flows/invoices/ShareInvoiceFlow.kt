package com.bfn.flows.invoices

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.ourIdentity
import com.template.states.InvoiceState
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import org.slf4j.LoggerFactory


@InitiatingFlow
@StartableByRPC
class ShareInvoiceFlow(private val invoiceId: String) : FlowLogic<Unit>() {

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): Unit {
        val serviceHub = serviceHub
        Companion.logger.info(" \uD83E\uDD1F \uD83E\uDD1F  \uD83E\uDD1F \uD83E\uDD1F  ... ShareInvoiceFlow call started ...")

        val page = serviceHub.vaultService.queryBy(
                criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED),
                contractStateType = InvoiceState::class.java,
                paging = PageSpecification(
                        pageNumber = 1, pageSize = 1000
                )
        )
        var invoiceState : StateAndRef<InvoiceState>? = null
        page.states.forEach() {
            if (it.state.data.invoiceId.toString() == invoiceId) {
                invoiceState = it
            }
        }
        val creationTransactionHash: SecureHash = invoiceState!!.ref.txhash
        val creationTransaction = serviceHub.validatedTransactions.getTransaction(creationTransactionHash)

        // Send the transaction to the counterParties.
        val meParty = serviceHub.ourIdentity
        val otherNodes = serviceHub.networkMapCache.allNodes

        Companion.logger.info("\uD83E\uDD16 \uD83E\uDD16 \uD83E\uDD16 \uD83E\uDD16 \uD83E\uDD16  " +
                "\uD83E\uDD16 Invoice sharing with  ${otherNodes.size - 2} other nodes!! \uD83E\uDD16 ")

        otherNodes.forEach() {
            val party = it.legalIdentities.first()
            if (party.name.organisation == meParty.name.organisation
                    || party.name.organisation.contains("Notary")) {
                logger.info("\uD83D\uDE0E \uD83D\uDE0E ignore notary and self and do not share invoice")
            } else {
                val counterPartySession = initiateFlow(party)
                subFlow(SendTransactionFlow(counterPartySession, creationTransaction!!))
                Companion.logger.info("\uD83E\uDD16 \uD83E\uDD16 \uD83E\uDD16 \uD83E\uDD16 \uD83E\uDD16  " +
                        "\uD83E\uDD16 Invoice has been shared with $party !! \uD83E\uDD16 ")
            }
        }

        return
    }


    companion object {
        private val logger = LoggerFactory.getLogger(ShareInvoiceFlow::class.java)
        private const val LOCAL_SUPPLIER = 1
        private const val LOCAL_investor = 2
        private const val REMOTE_SUPPLIER = 3
        private const val REMOTE_investor= 4
    }


}
