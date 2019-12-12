package com.bfn.flows.invoices

import co.paralleluniverse.fibers.Suspendable

import com.bfn.flows.regulator.ReportToRegulatorFlow
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService
import com.template.InvoiceOfferContract
import com.template.states.InvoiceOfferState
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.*

@InitiatingFlow
@StartableByRPC
class SelectBestInvoiceOfferFlow (private val supplierAccountId: String) : FlowLogic<SignedTransaction>() {

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
        Companion.logger.info(" \uD83E\uDD1F \uD83E\uDD1F  \uD83E\uDD1F \uD83E\uDD1F  ... BuyInvoiceOfferFlow call started ...")
        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
        val supplierAccount = accountService.accountInfo(UUID.fromString(supplierAccountId))!!.state.data
        val criteria = VaultQueryCriteria(
                status = StateStatus.UNCONSUMED,
                externalIds = listOf(supplierAccount.identifier.id))
        val page = serviceHub.vaultService.queryBy(
                contractStateType = InvoiceOfferState::class.java,
                paging = PageSpecification(pageNumber = 1, pageSize = 400),
                criteria = criteria).states

        val sorted = page.sortedBy { it.state.data.offerAmount }
        val selected = sorted.last().state.data
        val tx = subFlow(IssueInvoiceTokenFlow(
                amount = BigDecimal(selected.offerAmount),
                accountId = selected.investor.identifier.id.toString()))

        reportToRegulator(tx)
        Companion.logger.info("\uD83C\uDF6F \uD83C\uDF6F Best InvoiceOffer: ${selected.offerAmount} " +
                "by investor account : ${selected.investor.name} selected and tokens issued")
        //todo - consume all outstanding offers
        val notary  = serviceHub.networkMapCache.notaryIdentities.first()
        val command = InvoiceOfferContract.CloseOffers()
        page.forEach() {
            val mutableList = mutableListOf<StateRef>()
            mutableList.add(it.ref)
            val offer = it.state.data
            val txBuilder = TransactionBuilder(notary)
                    .addCommand(data = command, keys = listOf(
                            offer.supplier.host.owningKey,
                            offer.customer.host.owningKey,
                            offer.investor.host.owningKey))
                    .addInputState(stateAndRef = it)

            val partTx = serviceHub.signInitialTransaction(txBuilder)
            val session = initiateFlow(offer.investor.host)
            val tx1 = subFlow(CollectSignaturesFlow(partTx, listOf(session)))
            val tx2 = subFlow(FinalityFlow(transaction = tx1, sessions = listOf(session)))

            Companion.logger.info(" \uD83D\uDE21 \uD83D\uDE21 Closed offer from  ${it.state.data.investor.name} " +
                    "for  \uD83D\uDE21${it.state.data.offerAmount} \uD83D\uDC99  txId: ${tx2.id}")
        }

        return tx

    }

    @Suspendable
    @Throws(FlowException::class)
    private fun reportToRegulator(mSignedTransactionDone: SignedTransaction) {
        try {
            subFlow(ReportToRegulatorFlow(mSignedTransactionDone))
        } catch (e: Exception) {
            Companion.logger.error(" \uD83D\uDC7F  \uD83D\uDC7F  \uD83D\uDC7F Regulator fell down.", e)
            throw FlowException("Regulator fell down!")
        }
    }


    companion object {
        private val logger = LoggerFactory.getLogger(SelectBestInvoiceOfferFlow::class.java)
    }

}
