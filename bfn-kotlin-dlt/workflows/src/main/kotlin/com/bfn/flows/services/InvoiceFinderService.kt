package com.bfn.flows.services

import co.paralleluniverse.fibers.Suspendable
import com.bfn.flows.invoices.InvoiceCloseFlow
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService
import com.template.states.InvoiceState
import com.template.states.ProfileState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.Party
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.SingletonSerializeAsToken
import org.slf4j.LoggerFactory
import java.security.PublicKey
import java.util.*

@CordaService
class InvoiceFinderService(private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {
    private val accountService: KeyManagementBackedAccountService =
            serviceHub.cordaService(KeyManagementBackedAccountService::class.java)

    @Suspendable
     fun findInvoiceStateAndRef(invoiceId: String): StateAndRef<InvoiceState>? {

        var invoiceState: StateAndRef<InvoiceState>? = null
        val list = getAllInvoiceStateAnRefs()
        list.forEach() {
            if (invoiceId == it.state.data.invoiceId.toString()) {
                invoiceState = it
            }
        }

        return invoiceState!!
    }
    @Suspendable
    @Throws(Exception::class)
    fun findInvoicesForInvestor(investorId: String): List<InvoiceState> {
        logger.info("\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E InvoiceFinderService: findInvoices ... " +
                "\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E ")

        val account = accountService.accountInfo(UUID.fromString(investorId))?.state?.data
                ?: throw IllegalArgumentException("\uD83D\uDC7F Account not found")
        logger.info("\uD83D\uDCA6 Finding invoices for investor: \uD83D\uDC7D \uD83D\uDC7D " +
                " ${account.name} - ${account.host}")
        val sortedInvoices = getAllInvoices()
        val bestInvoices: MutableList<InvoiceState>? = mutableListOf()
        val profile = investorId?.let { findProfile(it) }
        if (profile == null) {
            logger.info("\uD83C\uDF36 No profile available, \uD83C\uDF36 " +
                    "returning all ${sortedInvoices.size} invoices found")
            return sortedInvoices
        }

        sortedInvoices.forEach(){
            if (it.totalAmount >= profile.minimumInvoiceAmount
                    && it.totalAmount <= profile.maximumInvoiceAmount ) {
                bestInvoices!!.add(it)
            }
        }
        return bestInvoices!!
    }
    @Suspendable
    fun findInvoicesForSupplier(supplierId: String): List<InvoiceState> {
        logger.info("\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E InvoiceFinderService: findInvoices ... " +
                "\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E ")

        val account = accountService.accountInfo(UUID.fromString(supplierId))?.state?.data
                ?: throw IllegalArgumentException("\uD83D\uDC7F Account not found")
        logger.info("\uD83D\uDCA6 Finding invoices for supplier: \uD83D\uDC7D \uD83D\uDC7D " +
                " ${account.name} - ${account.host}")
        val sortedInvoices = getAllInvoices()
        val supplierInvoices: MutableList<InvoiceState> = mutableListOf()

        sortedInvoices.forEach(){
            if (it.supplierInfo.name == account.name) {
                supplierInvoices.add(it)
            }
        }
        return supplierInvoices
    }
    @Suspendable
    fun getAllNodes(): List<Party> {
        val map: MutableMap<String, Party> = mutableMapOf()
        val nodes = serviceHub.networkMapCache.allNodes
        val keys: MutableList<PublicKey> = mutableListOf()
        nodes.forEach() {
            if (it.legalIdentities.first().toString().contains("Notary")) {
                logger.info(" ☕️  Notary ignored for consuming invoice")
            } else {
                map[it.legalIdentities.first().toString()] = it.legalIdentities.first()
            }
        }
        map.forEach() {
            keys.add(it.value.owningKey)
        }
        return map.values.toList()
    }
    @Suspendable
    fun findInvoicesForCustomer(customerId: String): List<InvoiceState> {
        logger.info("\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E InvoiceFinderService: findInvoices ... " +
                "\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E ")

        val account = accountService.accountInfo(UUID.fromString(customerId))?.state?.data
                ?: throw IllegalArgumentException("\uD83D\uDC7F Account not found")
        logger.info("\uD83D\uDCA6 Finding invoices for customer: \uD83D\uDC7D \uD83D\uDC7D " +
                " ${account.name} - ${account.host}")
        val sortedInvoices = getAllInvoices()
        val supplierInvoices: MutableList<InvoiceState>? = mutableListOf()

        sortedInvoices.forEach(){
            if (it.customerInfo.name == account.name) {
                supplierInvoices!!.add(it)
            }
        }
        return supplierInvoices!!
    }
    @Suspendable
    fun findInvoicesForNode(): List<InvoiceState> {
        logger.info("\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E InvoiceFinderService: findInvoices ... " +
                "\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E ")
        
        return getAllInvoices()
    }
    private val pageSize:Int = 5000

    @Suspendable
    fun findProfile(investorAccountId: String): ProfileState? {
        val criteria = QueryCriteria.VaultQueryCriteria(
                status = Vault.StateStatus.UNCONSUMED)
        logger.info("\uD83D\uDD35 Find investor profile ...")
        val page = serviceHub.vaultService.queryBy(
                contractStateType = ProfileState::class.java,
                paging = PageSpecification(pageNumber = 1, pageSize = 2000),
                criteria = criteria)
        var profile: ProfileState? = null
        page.states.forEach() {
            if (it.state.data.accountId == investorAccountId) {
                profile = it.state.data
            }
        }

        return profile!!
    }
    @Suspendable
    fun queryInvoices(pageNumber: Int): Vault.Page<InvoiceState> {
        val criteria = QueryCriteria.VaultQueryCriteria(
                status = Vault.StateStatus.UNCONSUMED)

        val page = serviceHub.vaultService.queryBy(
                contractStateType = InvoiceState::class.java,
                paging = PageSpecification(pageNumber = pageNumber, pageSize = pageSize),
                criteria = criteria)
        return page
    }
    @Suspendable
    fun queryInvoiceStateAndRef(pageNumber: Int): Pair<List<StateAndRef<InvoiceState>>, Long> {
        val criteria = QueryCriteria.VaultQueryCriteria(
                status = Vault.StateStatus.UNCONSUMED)

        val page = serviceHub.vaultService.queryBy(
                contractStateType = InvoiceState::class.java,
                paging = PageSpecification(pageNumber = pageNumber, pageSize = pageSize),
                criteria = criteria)
        return Pair(page.states, page.totalStatesAvailable )
    }
    @Suspendable
    private fun getAllInvoices(): List<InvoiceState> {
        val list: MutableList<InvoiceState> = mutableListOf()
        //get first page
        var pageNumber = 1
        val page: Vault.Page<InvoiceState> = queryInvoices(pageNumber)
        addToList(page = page, list = list)

        val remainder: Int = (page.totalStatesAvailable % pageSize).toInt()
        var pageCnt: Int = (page.totalStatesAvailable / pageSize).toInt()
        if (remainder > 0) pageCnt++

        if (pageCnt > 1)  {
            while (pageNumber < pageCnt) {
                pageNumber++
                val pageX = queryInvoices(pageNumber)
                addToList(pageX, list)
            }
        }
        val sorted = list.sortedBy { it.totalAmount }
        logger.info("\uD83E\uDDE9 Invoices found on node:  \uD83C\uDF00 ${sorted.size} " )
        return sorted
    }
    @Suspendable
    private fun getAllInvoiceStateAnRefs(): List<StateAndRef<InvoiceState>> {
        val list: MutableList<StateAndRef<InvoiceState>> = mutableListOf()
        //get first page
        var pageNumber = 1
        val pair = queryInvoiceStateAndRef(pageNumber)
        pair.first.forEach() {
            list.add(it)
        }

        val remainder: Int = (pair.second % pageSize).toInt()
        var pageCnt: Int = (pair.second/ pageSize).toInt()
        if (remainder > 0) pageCnt++

        if (pageCnt > 1)  {
            while (pageNumber < pageCnt) {
                pageNumber++
                val pageX = queryInvoiceStateAndRef(pageNumber)
                pageX.first.forEach() {
                    list.add(it)
                }
            }
        }
        val sorted = list.sortedBy { it.state.data.dateRegistered }
        logger.info("\uD83E\uDDE9 Invoices found on node:  \uD83C\uDF00 ${sorted.size} " )
        return sorted
    }

    @Suspendable
    private fun addToList(page: Vault.Page<InvoiceState>, list: MutableList<InvoiceState>) {
        page.states.forEach() {
            list.add(it.state.data)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(InvoiceFinderService::class.java)
    }

    init {
        logger.info("\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E InvoiceFinderService: " +
                "Investor finds invoices according to Profile. \uD83C\uDF4E \uD83C\uDF4E ")

    }
}
