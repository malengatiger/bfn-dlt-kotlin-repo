package com.bfn.flows.services

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService
import com.template.states.InvoiceState
import com.template.states.ProfileState
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.SingletonSerializeAsToken
import org.slf4j.LoggerFactory
import java.util.*

@CordaService
class InvoiceFinderService(private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {
    private val accountService: KeyManagementBackedAccountService =
            serviceHub.cordaService(KeyManagementBackedAccountService::class.java)

    @Suspendable
    @Throws(Exception::class)
    fun findInvoicesForInvestor(investorAccountId: String): List<InvoiceState> {
        logger.info("\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E InvoiceFinderService: findInvoices ... " +
                "\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E ")

        val account = accountService.accountInfo(UUID.fromString(investorAccountId))?.state?.data
                ?: throw IllegalArgumentException("\uD83D\uDC7F Account not found")
        logger.info("\uD83D\uDCA6 Finding invoices for investor: \uD83D\uDC7D \uD83D\uDC7D " +
                " ${account.name} - ${account.host}")
        val sortedInvoices = getInvoices()
        val bestInvoices: MutableList<InvoiceState>? = mutableListOf()
        val profile = investorAccountId?.let { findProfile(it) }
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
    fun findInvoicesForSupplier(supplierAccountId: String): List<InvoiceState> {
        logger.info("\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E InvoiceFinderService: findInvoices ... " +
                "\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E ")

        val account = accountService.accountInfo(UUID.fromString(supplierAccountId))?.state?.data
                ?: throw IllegalArgumentException("\uD83D\uDC7F Account not found")
        logger.info("\uD83D\uDCA6 Finding invoices for supplier: \uD83D\uDC7D \uD83D\uDC7D " +
                " ${account.name} - ${account.host}")
        val sortedInvoices = getInvoices()
        val supplierInvoices: MutableList<InvoiceState>? = mutableListOf()

        sortedInvoices.forEach(){
            if (it.supplierInfo.name == account.name) {
                supplierInvoices!!.add(it)
            }
        }
        return supplierInvoices!!
    }
    fun findInvoicesForNode(): List<InvoiceState> {
        logger.info("\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E InvoiceFinderService: findInvoices ... " +
                "\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E ")
        
        logger.info("\uD83D\uDCA6 Finding invoices for Node: \uD83D\uDC7D \uD83D\uDC7D ")
        return getInvoices()
    }
    private val pageSize:Int = 1000

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

        return serviceHub.vaultService.queryBy(
                contractStateType = InvoiceState::class.java,
                paging = PageSpecification(pageNumber = pageNumber, pageSize = pageSize),
                criteria = criteria)
    }
    @Suspendable
    private fun getInvoices(): List<InvoiceState> {
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
