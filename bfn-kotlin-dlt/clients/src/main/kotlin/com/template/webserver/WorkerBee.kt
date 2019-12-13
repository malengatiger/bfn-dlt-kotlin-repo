package com.template.webserver

import com.bfn.flows.CreateAccountFlow
import com.bfn.flows.invoices.SelectBestInvoiceOfferFlow
import com.bfn.flows.invoices.InvoiceOfferFlow
import com.bfn.flows.invoices.InvoiceRegistrationFlow
import com.google.firebase.cloud.FirestoreClient
import com.google.gson.GsonBuilder
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlow
import com.template.dto.*

import com.template.states.InvoiceOfferState
import com.template.states.InvoiceState
import com.template.states.InvoiceTokenType
import com.template.webserver.FirebaseUtil.addNode
import com.template.webserver.FirebaseUtil.createUser
import com.template.webserver.FirebaseUtil.deleteCollection
import com.template.webserver.FirebaseUtil.sendAccountMessage
import com.template.webserver.FirebaseUtil.sendInvoiceMessage
import com.template.webserver.FirebaseUtil.sendInvoiceOfferMessage
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import java.util.*
import java.util.concurrent.ExecutionException

object WorkerBee {
    private val logger = LoggerFactory.getLogger(WorkerBee::class.java)
    private val GSON = GsonBuilder().setPrettyPrinting().create()
    val db = FirestoreClient.getFirestore()

    @Throws(Exception::class)
    fun writeNodes(proxy: CordaRPCOps) {
        val nodes = listNodes(proxy)
        for (n in nodes) {
            addNode(n)
            logger.info("writeNodes:  🍎 🍎 🍎 node written to Firestore: 👽 " + n.addresses!![0])
        }
        logger.info("writeNodes:  🍎 🍎 🍎 nodes written to Firestore: 👽 👽 👽  " + nodes.size)
    }

    @JvmStatic
    fun listNodes(proxy: CordaRPCOps): List<NodeInfoDTO> {
        val nodes = proxy.networkMapSnapshot()
        val nodeList: MutableList<NodeInfoDTO> = ArrayList()
        for (info in nodes) {
            val dto = NodeInfoDTO()
            dto.serial = info.serial
            dto.platformVersion = info.platformVersion.toLong()
            for (party in info.legalIdentities) {
                dto.addresses = ArrayList()
                (dto.addresses as ArrayList<String>).add(party.name.toString())
            }
            logger.info("\uD83C\uDF3A BFN Corda Node: \uD83C\uDF3A "
                    + info.legalIdentities[0].name.toString())
            nodeList.add(dto)
        }
        logger.info(" \uD83E\uDDE1 \uD83D\uDC9B \uD83D\uDC9A Corda NetworkNodes found: \uD83D\uDC9A "
                + nodeList.size + " \uD83D\uDC9A ")
        return nodeList
    }

    @JvmStatic
    @Throws(ExecutionException::class, InterruptedException::class)
    fun listFirestoreNodes(): List<NodeInfoDTO> {
        val nodeList: MutableList<NodeInfoDTO> = ArrayList()
        val future = db.collection("nodes").get()
        val snapshots = future.get()
        val list = snapshots.documents
        for (snapshot in list) {
            val map = snapshot.data
            val node = NodeInfoDTO()
            node.webAPIUrl = map["webAPIUrl"] as String?
            node.serial = (map["serial"] as Long?)!!
            node.platformVersion = (map["platformVersion"] as Long?)!!
            node.addresses = ArrayList()
            (node.addresses as ArrayList<String>).add(map["addresses"].toString())
            nodeList.add(node)
        }
        return nodeList
    }

    @JvmStatic
    fun getAccounts(proxy: CordaRPCOps): List<AccountInfoDTO> {
        val accounts = proxy.vaultQuery(AccountInfo::class.java).states
        var cnt = 0
        val list: MutableList<AccountInfoDTO> = ArrayList()
        for ((state) in accounts) {
            cnt++
            //            logger.info(" \uD83C\uDF3A AccountInfo: #".concat("" + cnt + " :: ").concat(ref.getState().getData().toString()
//                    .concat(" \uD83E\uDD4F ")));
            val (name, host, identifier) = state.data
            val dto = AccountInfoDTO(identifier.id.toString(),
                    host.toString(), name, null)
            list.add(dto)
        }
        val msg = "\uD83C\uDF3A  \uD83C\uDF3A done listing accounts:  \uD83C\uDF3A " + list.size
        logger.info(msg)
        return list
    }

    @JvmStatic
    @Throws(Exception::class)
    fun getAccount(proxy: CordaRPCOps, accountId: String?): AccountInfoDTO {
        val list = getAccounts(proxy)
        var dto: AccountInfoDTO? = null
        for (info in list) {
            if (info.identifier.equals(accountId, ignoreCase = true)) {
                dto = info
                break
            }
        }
        if (dto == null) {
            logger.warn("Account not found on BFN account")
            throw Exception("Account not found on BFN network")
        }
        val msg = "\uD83C\uDF3A \uD83C\uDF3A found account:  \uD83C\uDF3A " + GSON.toJson(dto)
        logger.info(msg)
        return dto
    }

    @JvmStatic
    @Throws(Exception::class)
    fun getInvoiceStates(proxy: CordaRPCOps,
                         accountId: String?,
                         consumed: Boolean): List<InvoiceDTO> {
        logger.info("........................ accountId:  \uD83D\uDC9A " + if (accountId == null) "null" else "$accountId consumed:  \uD83D\uDC9A $consumed")
        val criteria: QueryCriteria = VaultQueryCriteria(
                if (consumed) StateStatus.CONSUMED else StateStatus.UNCONSUMED)
        val (states) = proxy.vaultQueryByWithPagingSpec(
                InvoiceState::class.java, criteria,
                PageSpecification(1, 200))
        val list: MutableList<InvoiceDTO> = ArrayList()
        logger.info("\uD83D\uDCA6 \uD83D\uDCA6 \uD83D\uDCA6 \uD83D\uDCA6 \uD83D\uDCA6 \uD83D\uDCA6 Total invoices found: " + states.size)
        var cnt = 0
        for ((state) in states) {
            val m = state.data
            val invoice = getDTO(m)
            cnt++
            //            logger.info("\uD83D\uDCA6 \uD83D\uDCA6 \uD83D\uDCA6 Invoice #"
//                    +cnt+" from stateAndRef, before check: " + GSON.toJson(invoice));
            if (accountId == null) {
                list.add(invoice)
                //                logger.warn("........... accountId is null ... list: " + list.size());
            } else {
                if (invoice.supplier!!.identifier.equals(accountId, ignoreCase = true)
                        || invoice.customer!!.identifier.equals(accountId, ignoreCase = true)) {
                    list.add(invoice)
                    //                    logger.warn("........... accountId is ".concat(accountId)
//                    .concat(" list: " + list.size()));
                }
            }
        }
        val m = " \uD83C\uDF3A  \uD83C\uDF3A  \uD83C\uDF3A  done listing InvoiceStates:  \uD83C\uDF3A " + list.size
        logger.info(m)
        return list
    }

    @JvmStatic
    @Throws(Exception::class)
    fun getInvoiceOfferStates(proxy: CordaRPCOps, accountId: String?, consumed: Boolean): List<InvoiceOfferDTO> {
        logger.info("...................... accountId:  \uD83D\uDC9A " + if (accountId == null) "null" else "$accountId consumed:  \uD83D\uDC9A $consumed")
        val criteria: QueryCriteria = VaultQueryCriteria(
                if (consumed) StateStatus.CONSUMED else StateStatus.ALL)
        val (states) = proxy.vaultQueryByWithPagingSpec(
                InvoiceOfferState::class.java, criteria,
                PageSpecification(1, 200))
        val list: MutableList<InvoiceOfferDTO> = ArrayList()
        logger.info("\uD83D\uDCA6 \uD83D\uDCA6 \uD83D\uDCA6 Total offers found: " + states.size)
        var cnt = 0
        for ((state) in states) {
            val offerState = state.data
            cnt++
            val offer = getDTO(offerState)
            if (accountId == null) {
                list.add(offer)
            } else {
                if (offer.supplier!!.identifier.equals(accountId, ignoreCase = true)
                        || offer.investor!!.identifier.equals(accountId, ignoreCase = true)
                        || offer.customer!!.identifier.equals(accountId, ignoreCase = true)) {
                    list.add(offer)
                }
            }
        }
        val m = "\uD83D\uDCA6  done listing InvoiceOfferStates:  \uD83C\uDF3A " + list.size
        logger.info(m)
        return list
    }

    //todo extend paging query where appropriate
    private const val PAGE_SIZE = 200

    @JvmStatic
    fun getDashboardData(proxy: CordaRPCOps): DashboardData {
        var pageNumber = 1
        val states: MutableList<StateAndRef<ContractState>> = ArrayList()
        val data = DashboardData()
        var totalResults: Long
        do {
            logger.info("\uD83E\uDDE9 \uD83E\uDDE9 \uD83E\uDDE9 \uD83E\uDDE9 \uD83E\uDDE9 " +
                    "processing page " + pageNumber)
            val pageSpec = PageSpecification(pageNumber, PAGE_SIZE)
            val criteria: QueryCriteria = VaultQueryCriteria(StateStatus.UNCONSUMED)
            val (newStates, _, totalStatesAvailable) = proxy.vaultQueryByWithPagingSpec(
                    ContractState::class.java, criteria, pageSpec)
            totalResults = totalStatesAvailable
            logger.info("\uD83D\uDCA6 \uD83D\uDCA6 Number of States \uD83C\uDF4E " + newStates.size)
            states.addAll(newStates)
            pageNumber++
        } while (PAGE_SIZE * (pageNumber - 1) <= totalResults)
        var accts = 0
        var invoices = 0
        var offers = 0
        var acctsp = 0
        var invoicesp = 0
        var offersp = 0
        val mList: MutableList<String> = ArrayList()
        for ((state1) in states) {
            val state = state1.data
            val m = "\uD83E\uDDE9 \uD83E\uDDE9 " + "State class: " + state.javaClass.name + " participants: " + state.participants.size
            if (m.contains("AccountInfo")) {
                accts++
                acctsp = state.participants.size
            }
            if (m.contains("InvoiceState")) {
                invoices++
                invoicesp = state.participants.size
            }
            if (m.contains("InvoiceOfferState ")) {
                offers++
                offersp = state.participants.size
            }
        }
        val info = proxy.nodeInfo()
        data.node = info.legalIdentities[0].name.toString()
        data.accounts = accts
        data.invoices = invoices
        data.offers = offers
        val t1 = "\n\n\uD83E\uDDE9 \uD83E\uDDE9 List of States on " + info.legalIdentities[0].name.toString()
        val a1 = "\uD83E\uDDE9 \uD83E\uDDE9 AccountInfo found on node: \uD83C\uDF4E $accts \uD83C\uDF4E partcipants:  \uD83E\uDDE1 $acctsp"
        val a2 = "\uD83E\uDDE9 \uD83E\uDDE9 InvoiceStates found on node: \uD83C\uDF4E $invoices \uD83C\uDF4E  partcipants:  \uD83E\uDDE1 $invoicesp"
        val a3 = "\uD83E\uDDE9 \uD83E\uDDE9 InvoiceOfferStates found on node: \uD83C\uDF4E $offers \uD83C\uDF4E  partcipants:  \uD83E\uDDE1 $offersp"
        mList.add(t1)
        mList.add(a1)
        mList.add(a2)
        mList.add(a3)
        mList.add("\uD83E\uDDE9 \uD83E\uDDE9 Total states found:  \uD83E\uDDE1 " + (accts + invoices + offers) + "  \uD83E\uDDE1 \n\n")
        for (m in mList) {
            logger.info(m)
        }
        return data
    }

    @JvmStatic
    fun getStates(proxy: CordaRPCOps): List<String> {
        var pageNumber = 1
        val states: MutableList<StateAndRef<ContractState>> = ArrayList()
        var totalResults: Long
        do {
            logger.info("\uD83E\uDDE9 \uD83E\uDDE9 \uD83E\uDDE9 \uD83E\uDDE9 \uD83E\uDDE9 " +
                    "processing page " + pageNumber)
            val pageSpec = PageSpecification(pageNumber, PAGE_SIZE)
            val criteria: QueryCriteria = VaultQueryCriteria(StateStatus.UNCONSUMED)
            val (newStates, _, totalStatesAvailable) = proxy.vaultQueryByWithPagingSpec(
                    ContractState::class.java, criteria, pageSpec)
            totalResults = totalStatesAvailable
            logger.info("\uD83D\uDCA6 \uD83D\uDCA6 Number of States \uD83C\uDF4E " + newStates.size)
            states.addAll(newStates)
            pageNumber++
        } while (PAGE_SIZE * (pageNumber - 1) <= totalResults)
        var accts = 0
        var invoices = 0
        var offers = 0
        var acctsp = 0
        var invoicesp = 0
        var offersp = 0
        val mList: MutableList<String> = ArrayList()
        for ((state1) in states) {
            val state = state1.data
            val m = "\uD83E\uDDE9 \uD83E\uDDE9 " + "State class: " + state.javaClass.name + " participants: " + state.participants.size
            if (m.contains("AccountInfo")) {
                accts++
                acctsp = state.participants.size
            }
            if (m.contains("InvoiceState")) {
                invoices++
                invoicesp = state.participants.size
            }
            if (m.contains("InvoiceOfferState ")) {
                offers++
                offersp = state.participants.size
            }
        }
        val info = proxy.nodeInfo()
        val t1 = "\n\n\uD83E\uDDE9 \uD83E\uDDE9 List of States on " + info.legalIdentities[0].name.toString()
        val a1 = "\uD83E\uDDE9 \uD83E\uDDE9 AccountInfo found on node: \uD83C\uDF4E $accts \uD83C\uDF4E participants:  \uD83E\uDDE1 $acctsp"
        val a2 = "\uD83E\uDDE9 \uD83E\uDDE9 InvoiceStates found on node: \uD83C\uDF4E $invoices \uD83C\uDF4E  participants:  \uD83E\uDDE1 $invoicesp"
        val a3 = "\uD83E\uDDE9 \uD83E\uDDE9 InvoiceOfferStates found on node: \uD83C\uDF4E $offers \uD83C\uDF4E  participants:  \uD83E\uDDE1 $offersp"
        mList.add(t1)
        mList.add(a1)
        mList.add(a2)
        mList.add(a3)
        mList.add("\uD83E\uDDE9 \uD83E\uDDE9 Total states found:  \uD83E\uDDE1 " + (accts + invoices + offers) + "  \uD83E\uDDE1 \n\n")
        for (m in mList) {
            logger.info(m)
        }
        return mList
    }

    @JvmStatic
    fun listFlows(proxy: CordaRPCOps): List<String> {
        logger.info("🥬 🥬 🥬 🥬 Registered Flows on Corda BFN ...  \uD83E\uDD6C ")
        val flows = proxy.registeredFlows()
        var cnt = 0
        for (info in flows) {
            cnt++
            logger.info("\uD83E\uDD4F \uD83E\uDD4F #$$cnt \uD83E\uDD6C BFN Corda Flow:  \uD83E\uDD4F$info   \uD83C\uDF4E ")
        }
        logger.info("🥬 🥬 🥬 🥬 Total Registered Flows  \uD83C\uDF4E  $cnt  \uD83C\uDF4E \uD83E\uDD6C ")
        return flows
    }

    @JvmStatic
    fun listNotaries(proxy: CordaRPCOps): List<String> {
        val notaryIdentities = proxy.notaryIdentities()
        val list: MutableList<String> = ArrayList()
        for (info in notaryIdentities) {
            logger.info(" \uD83D\uDD35  \uD83D\uDD35 BFN Corda Notary: \uD83C\uDF3A " + info.name.toString())
            list.add(info.name.toString())
        }
        return list
    }

    @JvmStatic
    @Throws(Exception::class)
    fun startInvoiceRegistrationFlow(proxy: CordaRPCOps, invoice: InvoiceDTO): InvoiceDTO {
        return try {
            val accounts = proxy.vaultQuery(AccountInfo::class.java).states
            var supplierInfo: AccountInfo? = null
            var customerInfo: AccountInfo? = null
            for ((state) in accounts) {
                if (state.data.identifier.toString().equals(invoice.customer!!.identifier, ignoreCase = true)) {
                    customerInfo = state.data
                }
                if (state.data.identifier.toString().equals(invoice.supplier!!.identifier, ignoreCase = true)) {
                    supplierInfo = state.data
                }
            }
            if (supplierInfo == null) {
                throw Exception("Supplier is fucking missing")
            }
            if (customerInfo == null) {
                throw Exception("Customer is bloody missing")
            }
            val taxPercentage = (invoice.valueAddedTax?.div(100.0) ?: 0.0)
            val totalTaxAmt = invoice.amount!! * taxPercentage
            invoice.totalAmount = totalTaxAmt + invoice.amount!!;
            //
            val invoiceState = InvoiceState(UUID.randomUUID(),
                    invoice.invoiceNumber!!,
                    invoice.description!!,
                    invoice.amount!!,
                    invoice.valueAddedTax!!,
                    invoice.totalAmount!!,
                    supplierInfo,
                    customerInfo,
                    Date())
            val signedTransactionCordaFuture = proxy.startTrackedFlowDynamic(
                    InvoiceRegistrationFlow::class.java, invoiceState).returnValue
            val issueTx = signedTransactionCordaFuture.get()
            logger.info("\uD83C\uDF4F \uD83C\uDF4F \uD83C\uDF4F \uD83C\uDF4F flow completed... " +
                    "\uD83C\uDF4F \uD83C\uDF4F \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06  " +
                    "\uD83D\uDC4C \uD83D\uDC4C \uD83D\uDC4C  signedTransaction returned: \uD83E\uDD4F "
                    + issueTx.toString() + " \uD83E\uDD4F \uD83E\uDD4F ")
            val dto = getDTO(invoiceState)
            //logger.info("Check amount discount total calculations: " + GSON.toJson(dto))
            try {
                sendInvoiceMessage(dto)
                val reference = db.collection("invoices").add(dto)
                logger.info("\uD83E\uDDE9\uD83E\uDDE9\uD83E\uDDE9\uD83E\uDDE9\uD83E\uDDE9\uD83E\uDDE9 " +
                        "Firestore path: " + reference.get().path)
            } catch (e: Exception) {
                logger.error(e.message)
            }
            dto
        } catch (e: Exception) {
            if (e.message != null) {
                throw Exception("Failed to register invoice. " + e.message)
            } else {
                throw Exception("Failed to register invoice. Unknown cause")
            }
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun startAccountRegistrationFlow(proxy: CordaRPCOps,
                                     accountName: String, email: String?, password: String?,
                                     cellphone: String?): AccountInfoDTO {
        return try {
            val criteria: QueryCriteria = VaultQueryCriteria(StateStatus.UNCONSUMED)
            val (states) = proxy.vaultQueryByWithPagingSpec(
                    AccountInfo::class.java, criteria,
                    PageSpecification(1, 200))
            logger.info(" \uD83E\uDDA0 \uD83E\uDDA0 Accounts found on network:  \uD83E\uDD6C " + states.size)
            for ((state) in states) {
                val info = state.data
                if (info.name.equals(accountName, ignoreCase = true)) {
                    logger.info("Account $accountName \uD83D\uDC7F \uD83D\uDC7F already exists on the network")
                    throw Exception("Account already exists on the network")
                }
            }
            val accountInfoCordaFuture = proxy.startTrackedFlowDynamic(
                    CreateAccountFlow::class.java, accountName).returnValue
            val (name, host, identifier) = accountInfoCordaFuture.get()
            logger.info("\uD83C\uDF4F \uD83C\uDF4F \uD83C\uDF4F \uD83C\uDF4F Flow completed... " +
                    " \uD83D\uDC4C \uD83D\uDC4C " +
                    "\uD83D\uDC4C accountInfo returned: \uD83E\uDD4F " + name + " \uD83E\uDD4F \uD83E\uDD4F ")
            //create user record in firebase
            try {
                createUser(accountName, email, password,
                        cellphone, identifier.id.toString())

            } catch (e: Exception) {
                logger.error(e.message)
                logger.error("Firebase fucked up ......")
                throw e
            }
            val dto = AccountInfoDTO()
            dto.host = host.toString()
            dto.identifier = identifier.id.toString()
            dto.name = name
            try {
                sendAccountMessage(dto)
                val reference = db.collection("accounts").add(dto)
                logger.info("\uD83E\uDDE9\uD83E\uDDE9\uD83E\uDDE9\uD83E\uDDE9\uD83E\uDDE9\uD83E\uDDE9 " +
                        "Firestore path: " + reference.get().path)
            } catch (e: Exception) {
                logger.error(e.message)
            }
            dto
        } catch (e: Exception) {
            logger.info("\uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D " +
                    "Houston, we have fallen over! Account creation failed")
            logger.error(e.message)
            throw e
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun startInvoiceOfferFlow(proxy: CordaRPCOps, invoiceOffer: InvoiceOfferDTO): InvoiceOfferDTO {
        return try {
            logger.info(" \uD83E\uDDE9 \uD83E\uDDE9 \uD83E\uDDE9 \uD83E\uDDE9 startInvoiceOfferFlow ....")
            //todo - refactor to proper query ...
            val criteria: QueryCriteria = VaultQueryCriteria(StateStatus.UNCONSUMED)
            val (states) = proxy.vaultQueryByWithPagingSpec(
                    InvoiceState::class.java, criteria,
                    PageSpecification(1, 200))
            var invoiceState: InvoiceState? = null
            for (state in states) {
                if (state.state.data.invoiceId.toString().equals(invoiceOffer.invoiceId, ignoreCase = true)) {
                    invoiceState = state.state.data
                    break
                }
            }
            if (invoiceState == null) {
                logger.warn("InvoiceState not found, \uD83D\uDC7F offer probably made on foreign node")
                throw Exception("Invoice not found")
            }
            var investorInfo: AccountInfo? = null
            val (states1) = proxy.vaultQueryByWithPagingSpec(
                    AccountInfo::class.java, criteria,
                    PageSpecification(1, 200))
            for ((state) in states1) {
                if (state.data.identifier.toString().equals(invoiceOffer.investor!!.identifier, ignoreCase = true)) {
                    investorInfo = state.data
                }
            }
            if (investorInfo == null) {
                throw Exception("Investor not found")
            }
            if (invoiceOffer.discount == Double.MIN_VALUE) {
                throw Exception("Discount not found")
            }
            val nPercentage = 100.0 - invoiceOffer.discount!!
            invoiceOffer.offerAmount = invoiceOffer.originalAmount!! * (nPercentage / 100)
            processInvoiceOffer(proxy, invoiceOffer, invoiceState, investorInfo)
        } catch (e: Exception) {
            if (e.message != null) {
                throw Exception("Failed to register invoiceOffer.  \uD83D\uDC7F ${e.message}")
            } else {
                throw Exception("Failed to register invoiceOffer. Unknown cause")
            }
        }
    }

    @Throws(Exception::class)
    fun startInvoiceOfferFlowToAllAccounts(proxy: CordaRPCOps, all: InvoiceOfferAllDTO): List<InvoiceOfferDTO> {
        return try {
            if (all.discount == Double.MIN_VALUE) {
                throw Exception("Discount not found")
            }
            //todo - refactor to proper query ...
            val criteria: QueryCriteria = VaultQueryCriteria(StateStatus.UNCONSUMED)
            val (states) = proxy.vaultQueryByWithPagingSpec(
                    InvoiceState::class.java, criteria,
                    PageSpecification(1, 200))
            var invoiceState: InvoiceState? = null
            for (state in states) {
                if (state.state.data.invoiceId.toString().equals(all.invoiceId, ignoreCase = true)) {
                    invoiceState = state.state.data
                    break
                }
            }
            if (invoiceState == null) {
                throw Exception("Invoice not found")
            }
            val (states1) = proxy.vaultQueryByWithPagingSpec(
                    AccountInfo::class.java, criteria,
                    PageSpecification(1, 200))
            val m = getAccount(proxy, all.accountId)
            val offers: MutableList<InvoiceOfferDTO> = ArrayList()
            //
            val invoiceOffer = InvoiceOfferDTO()
            invoiceOffer.invoiceId = all.invoiceId
            invoiceOffer.invoiceNumber = invoiceState.invoiceNumber
            invoiceOffer.offerAmount = invoiceOffer.offerAmount
            invoiceOffer.discount = invoiceOffer.discount
            invoiceOffer.supplier = m
            invoiceOffer.owner = m
            invoiceOffer.originalAmount = invoiceState.amount
            invoiceOffer.offerDate = Date()
            val n = 100.0 - (invoiceOffer.discount?.div(100)!!)
            invoiceOffer.offerAmount = invoiceOffer.originalAmount!! * n
            logger.info("\uD83D\uDC7D \uD83D\uDC7D INVOICE: " + invoiceOffer.invoiceId)
            logger.info("we have to send offer  to " + (states1.size - 1) + " accounts")
            for (info in states1) {
                if (info.state.data.identifier.id.toString().equals(all.accountId, ignoreCase = true)) {
                    logger.info("\uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 Ignore this account :: \uD83D\uDE21  " + info.state.data.name)
                    continue
                }
                invoiceOffer.investor = getDTO(info.state.data)
                val offerDTO = processInvoiceOffer(proxy, invoiceOffer,
                        invoiceState, info.state.data)
                offers.add(offerDTO)
            }
            offers
        } catch (e: Exception) {
            if (e.message != null) {
                throw Exception("Failed to add invoiceOffers. " + e.message)
            } else {
                throw Exception("Failed to add invoiceOffers. Unknown cause")
            }
        }
    }

    @Throws(Exception::class)
    private fun processInvoiceOffer(proxy: CordaRPCOps, invoiceOffer: InvoiceOfferDTO, invoiceState: InvoiceState, investorInfo: AccountInfo): InvoiceOfferDTO {
        val invoiceOfferState = InvoiceOfferState(
                invoiceId = invoiceState.invoiceId,
                customer = invoiceState.customerInfo,
                investor = investorInfo,
                supplier = invoiceState.supplierInfo,
                discount = invoiceOffer.discount!!,
                invoiceNumber = invoiceState.invoiceNumber,
                offerAmount = invoiceOffer.offerAmount!!,
                offerDate = Date(),
                originalAmount = invoiceState.totalAmount,
                ownerDate = Date()
        )
        val signedTransactionCordaFuture = proxy.startTrackedFlowDynamic(
                InvoiceOfferFlow::class.java, invoiceOfferState)
                .returnValue
        val issueTx = signedTransactionCordaFuture.get()
        logger.info("\uD83C\uDF4F \uD83C\uDF4F processInvoiceOffer completed... " +
                "\uD83C\uDF4F \uD83C\uDF4F \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDC4C " +
                "\uD83D\uDC4C \uD83D\uDC4C \uD83D\uDC4C  signedTransaction returned: \uD83E\uDD4F " + issueTx.toString() + " \uD83E\uDD4F \uD83E\uDD4F ")
        val offerDTO = invoiceOfferState?.let { getDTO(it) }
        try {
            val reference = db.collection("invoiceOffers").add(offerDTO)
            logger.info("\uD83E\uDDE9\uD83E\uDDE9\uD83E\uDDE9\uD83E\uDDE9\uD83E\uDDE9\uD83E\uDDE9 " +
                    "Firestore invoiceOffers path: " + reference.get().path)
        } catch (e: Exception) {
            logger.error(e.message)
        }
        sendInvoiceOfferMessage(offerDTO)
        return offerDTO!!
    }

    @JvmStatic
    @Throws(Exception::class)
    fun getDTO(state: InvoiceState): InvoiceDTO {
        val invoice = InvoiceDTO()
        invoice.amount = state.amount
        invoice.customer = getDTO(state.customerInfo)
        invoice.supplier = getDTO(state.supplierInfo)
        invoice.description = state.description
        invoice.invoiceId = state.invoiceId.toString()
        invoice.invoiceNumber = state.invoiceNumber
        invoice.dateRegistered = state.dateRegistered
        invoice.valueAddedTax = state.valueAddedTax
        invoice.totalAmount = state.totalAmount
        return invoice
    }

    @JvmStatic
    @Throws(Exception::class)
    fun getDTO(state: InvoiceOfferState): InvoiceOfferDTO {
        val o = InvoiceOfferDTO()
        o.invoiceId = state.invoiceId.toString()
        o.invoiceNumber = state.invoiceNumber
        o.offerAmount = state.offerAmount
        o.originalAmount = state.originalAmount
        o.discount = state.discount
        o.supplier = getDTO(state.supplier)
        o.investor = getDTO(state.investor)
        o.customer = getDTO(state.customer)

        if (state.offerDate != null) {
            o.offerDate = state.offerDate
        }
        if (state.ownerDate != null) {
            o.investorDate = state.ownerDate
        }
        return o
    }
    @JvmStatic
    fun getDTO(a: AccountInfo): AccountInfoDTO {
        val info = AccountInfoDTO()
        info.host = a.host.toString()
        info.identifier = a.identifier.id.toString()
        info.name = a.name
        return info
    }
}
