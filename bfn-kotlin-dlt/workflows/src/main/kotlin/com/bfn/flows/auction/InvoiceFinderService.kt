package com.bfn.flows.auction

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService
import com.template.states.InvoiceOfferState
import com.template.states.InvoiceState
import com.template.states.ProfileState
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.SingletonSerializeAsToken
import org.slf4j.LoggerFactory

@CordaService
class InvoiceFinderService(private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {
    private val accountService: KeyManagementBackedAccountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)

    @Suspendable
    @Throws(Exception::class)
    fun findBestInvoices(supplierAccountId: String,
                      invoiceId: String): List<InvoiceState> {
        logger.info("\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E InvoiceFinderService:selectBestOffer ... " +
                "\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E ")
        this.supplierAccountId = supplierAccountId
        this.invoiceId = invoiceId

        val sortedOffers = filterOffersByParams()
        var bestInvoices: MutableList<InvoiceState>? = mutableListOf()
        //
        val profile = findProfile()

        //todo - use profile to filter offers
        //todo - if ties found - take random offer

        return bestInvoices!!
    }
    @Suspendable
    private fun selectOffer(profile: ProfileState) : InvoiceOfferState{
        var bestOffer: InvoiceOfferState? = null

        return bestOffer!!
    }
    private val pageSize:Int = 1000
    private lateinit var supplierAccountId: String
    private lateinit var invoiceId: String
    @Suspendable
    fun findProfile(): ProfileState {
        val criteria = QueryCriteria.VaultQueryCriteria(
                status = Vault.StateStatus.UNCONSUMED)

        val page = serviceHub.vaultService.queryBy(
                contractStateType = ProfileState::class.java,
                paging = PageSpecification(pageNumber = 1, pageSize = 2000),
                criteria = criteria)
        var profile: ProfileState? = null
        page.states.forEach() {
            if (it.state.data.accountId == supplierAccountId) {
                profile = it.state.data
            }
        }
        return profile!!
    }
    @Suspendable
    fun queryOffers(pageNumber: Int): Vault.Page<InvoiceOfferState> {
        val criteria = QueryCriteria.VaultQueryCriteria(
                status = Vault.StateStatus.UNCONSUMED)

        return serviceHub.vaultService.queryBy(
                contractStateType = InvoiceOfferState::class.java,
                paging = PageSpecification(pageNumber = pageNumber, pageSize = pageSize),
                criteria = criteria)
    }
    @Suspendable
    private fun filterOffersByParams(): List<StateAndRef<InvoiceOfferState>> {
        val list: MutableList<StateAndRef<InvoiceOfferState>> = ArrayList()
        //get first page
        var pageNumber = 1
        val page: Vault.Page<InvoiceOfferState> = queryOffers(pageNumber)
        addToList(page = page, list = list)

        val remainder: Int = (page.totalStatesAvailable % pageSize).toInt()
        var pageCnt: Int = (page.totalStatesAvailable / pageSize).toInt()
        if (remainder > 0) pageCnt++

        if (pageCnt > 1)  {
            while (pageNumber < pageCnt) {
                pageNumber++
                val pageX = queryOffers(pageNumber)
                addToList(pageX, list)
            }
        }
        val sorted = list.sortedBy { it.state.data.offerAmount }
        logger.info("\uD83E\uDDE9 InvoiceOffers found for invoice:  \uD83C\uDF00 ${sorted.size} " )

        return sorted
    }

    @Suspendable
    private fun addToList(page: Vault.Page<InvoiceOfferState>, list: MutableList<StateAndRef<InvoiceOfferState>>) {
        page.states.forEach() {
            if (it.state.data.supplier.identifier.id.toString() == supplierAccountId
                    && it.state.data.invoiceId.toString() == invoiceId) {
                list.add(it)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(InvoiceFinderService::class.java)
    }

    init {
        logger.info("\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E InvoiceFinderService: " +
                "Investor finds best invoices via Profile. \uD83C\uDF4E \uD83C\uDF4E ")

    }
}
