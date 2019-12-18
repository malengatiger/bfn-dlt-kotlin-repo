package com.template
import com.google.gson.GsonBuilder
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.template.states.InvoiceOfferState
import com.template.states.InvoiceState
import com.template.states.OfferAndTokenState
import com.template.webserver.WorkerBee
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.loggerFor
import khttp.get as httpGet


/**
 * Connects to a Corda node via RPC and performs RPC operations on the node.
 *
 * The RPC connection is configured using command line arguments.
 */
fun main(args: Array<String>) = Client().main(args)

private class Client {
    companion object {
        val logger = loggerFor<Client>()
        private val GSON = GsonBuilder().setPrettyPrinting().create()

    }


    fun main(args: Array<String>) {

        val nodeAddressNotary = NetworkHostAndPort(host = "localhost", port = 10003)
        val nodeAddressPartyA = NetworkHostAndPort(host = "localhost", port = 10006)
        val nodeAddressPartyB = NetworkHostAndPort(host = "localhost", port = 10009)
        val nodeAddressPartyC = NetworkHostAndPort(host = "localhost", port = 10012)
        val nodeAddressRegulator = NetworkHostAndPort(host = "localhost", port = 10015)
        val rpcUsername = "user1"
        val rpcPassword = "test"

        val clientNotary = CordaRPCClient(nodeAddressNotary)
        val proxyNotary = clientNotary.start(rpcUsername, rpcPassword).proxy
        getThisNode(proxyNotary)

        val clientA = CordaRPCClient(nodeAddressPartyA)
        val proxyPartyA = clientA.start(rpcUsername, rpcPassword).proxy
        getThisNode(proxyPartyA)

        val clientB = CordaRPCClient(nodeAddressPartyB)
        val proxyPartyB = clientB.start(rpcUsername, rpcPassword).proxy
        getThisNode(proxyPartyB)

        val clientC = CordaRPCClient(nodeAddressPartyC)
        val proxyPartyC = clientC.start(rpcUsername, rpcPassword).proxy
        getThisNode(proxyPartyC)

        val clientReg = CordaRPCClient(nodeAddressRegulator)
        val proxyReg = clientReg.start(rpcUsername, rpcPassword).proxy

        getThisNode(proxyReg)
        doNodesAndAggregates(proxyPartyA, proxyPartyB, proxyPartyC, proxyReg)

//        getNodeTotals(proxyPartyA)
//        getNodeTotals(proxyPartyB)
//        getNodeTotals(proxyPartyC)
//        getNodeTotals(proxyReg)
//
//        startAccounts(true, deleteFirestore = true);
//        generateInvoices(0)
//        generateInvoices(1)
//        generateInvoices(2)

//        generateCrossNodeInvoices(0, 1)
        generateCrossNodeInvoices(1, 2)
//        generateCrossNodeInvoices(2, 2)
////
//        generateOffers(0)
//        generateOffers(1)
//        generateOffers(2)

//        runInvoiceOfferAuction(proxyPartyA)
//        runInvoiceOfferAuction(proxyPartyB)
//        runInvoiceOfferAuction(proxyPartyC)
////
        getNodeTotals(proxyPartyA)
        getNodeTotals(proxyPartyB)
        getNodeTotals(proxyPartyC)
        getNodeTotals(proxyReg)

        getOfferAndTokens(proxyPartyA)
        logger.info("\n \uD83C\uDF38  \uD83C\uDF38  \uD83C\uDF38  \uD83C\uDF38  \uD83C\uDF38 ")
        getOfferAndTokens(proxyPartyB)
        logger.info("\n \uD83C\uDF38  \uD83C\uDF38  \uD83C\uDF38  \uD83C\uDF38  \uD83C\uDF38 ")
        getOfferAndTokens(proxyPartyC)
        logger.info("\n \uD83C\uDF38  \uD83C\uDF38  \uD83C\uDF38  \uD83C\uDF38  \uD83C\uDF38 ")
        getOfferAndTokens(proxyReg)

//        getTokens(proxyPartyA)
//        getTokens(proxyPartyB)
//        getTokens(proxyPartyC)
//        getTokens(proxyReg)
    }
    fun getRegulatorTotals(proxy: CordaRPCOps) {
        logger.info("\n\uD83C\uDF3A \uD83C\uDF3A \uD83C\uDF3A \uD83C\uDF3A REGULATOR STATES \uD83C\uDF3A ")
        val page = proxy.vaultQuery(AccountInfo::class.java)
        logger.info("\uD83C\uDF3A Total Accounts on Regulator: ${page.states.size} \uD83C\uDF3A ")
        val page1 = proxy.vaultQuery(InvoiceState::class.java)
        logger.info("\uD83C\uDF3A Total InvoiceStates on Regulator: ${page1.states.size} \uD83C\uDF3A ")
        val page2 = proxy.vaultQuery(InvoiceOfferState::class.java)
        logger.info("\uD83C\uDF3A Total InvoiceOfferStates on Regulator: ${page2.states.size} \uD83C\uDF3A ")
        val page3 = proxy.vaultQuery(OfferAndTokenState::class.java)
        logger.info("\uD83C\uDF3A Total OfferAndTokenStates on Regulator: ${page3.states.size} \uD83C\uDF3A \n")
    }
    fun getNodeTotals(proxy: CordaRPCOps) {
        val name = proxy.nodeInfo().legalIdentities.first().name.organisation
        logger.info("\n\uD83C\uDF3A \uD83C\uDF3A \uD83C\uDF3A \uD83C\uDF3A ${name.toUpperCase()} STATES \uD83C\uDF3A ")
        val page = proxy.vaultQuery(AccountInfo::class.java)
        logger.info("\uD83C\uDF3A Total Accounts on ${name.toUpperCase()}: ${page.states.size} \uD83C\uDF3A ")
        val page1 = proxy.vaultQuery(InvoiceState::class.java)
        logger.info("\uD83C\uDF3A Total InvoiceStates on ${name.toUpperCase()}: ${page1.states.size} \uD83C\uDF3A ")
        val page2 = proxy.vaultQuery(InvoiceOfferState::class.java)
        logger.info("\uD83C\uDF3A Total InvoiceOfferStates on ${name.toUpperCase()}: ${page2.states.size} \uD83C\uDF3A ")
        val page3 = proxy.vaultQuery(OfferAndTokenState::class.java)
        logger.info("\uD83C\uDF3A Total OfferAndTokenStates on ${name.toUpperCase()}: ${page3.states.size} \uD83C\uDF3A \n")
    }
    fun getTokens(proxy: CordaRPCOps) {
        val criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        val page =
        proxy.vaultQueryByWithPagingSpec(contractStateType = FungibleToken::class.java, criteria = criteria,
                paging = PageSpecification(pageNumber = 1, pageSize = 200))
        logger.info("\uD83D\uDE3C \uD83E\uDDE9 \uD83E\uDDE9 Tokens on Node: \uD83E\uDDE9 \uD83E\uDDE9 " +
                "${proxy.nodeInfo().legalIdentities.first()} \uD83D\uDE3C ${page.totalStatesAvailable} \uD83D\uDE3C ")
        page.states.forEach() {
            logger.info("\uD83D\uDE3C \uD83D\uDE3C ${it.state.data}  \uD83C\uDF51 ")
        }
    }
    fun getOfferAndTokens(proxy: CordaRPCOps) {
        val criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        val page =
                proxy.vaultQueryByWithPagingSpec(contractStateType = OfferAndTokenState::class.java, criteria = criteria,
                        paging = PageSpecification(pageNumber = 1, pageSize = 200))
        logger.info("\uD83D\uDE3C \uD83E\uDDE9 \uD83E\uDDE9 Tokens on Node: \uD83E\uDDE9 \uD83E\uDDE9 " +
                "${proxy.nodeInfo().legalIdentities.first()} \uD83D\uDE3C ${page.totalStatesAvailable} \uD83D\uDE3C ")

        val sorted = page.states.sortedBy { it.state.data.invoiceOffer.investor.host.toString() }
        sorted.forEach() {
            logger.info("\uD83D\uDE3C \uD83D\uDE3C Investor: ${it.state.data.invoiceOffer.investor.host.name.organisation} " +
                    "\uD83C\uDF51 ${it.state.data.invoiceOffer.investor.name} \uD83C\uDF51 " +
                    " from supplier: \uD83D\uDD35  ${it.state.data.invoiceOffer.supplier.host.name.organisation} \uD83D\uDD35 ${it.state.data.invoiceOffer.supplier.name}" +
                    " invoiceAmt: ${it.state.data.invoiceOffer.originalAmount} :discount: ${it.state.data.invoiceOffer.discount} " +
                    " \uD83D\uDECE Token amount: ${it.state.data.token.amount} ")
        }
    }
    private fun startAccounts(generateAccounts: Boolean = false, deleteFirestore: Boolean = false) {
        if (generateAccounts) {
            logger.info(" \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 accounts for PARTY A")
            var status = startAccountsForNode(url = "http://localhost:10050",
                    deleteFirestore = deleteFirestore)
            if(status == 200) {
                logger.info(" \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C Successfully generated Party A")
            } else {
                logger.info("Houston, we down, \uD83D\uDCA6 status :  $status ")
            }

            logger.info(" \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 accounts for PARTY B")
            status = startAccountsForNode(url = "http://localhost:10053",
                    deleteFirestore = false)
            if(status == 200) {
                logger.info(" \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C Successfully generated Party B")
            } else {
                logger.info("Houston, we down, \uD83D\uDCA6 status :  $status ")
            }
            logger.info(" \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 accounts for PARTY C")
            status = startAccountsForNode(url = "http://localhost:10056",
                    deleteFirestore = false)
            if(status == 200) {
                logger.info(" \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C Successfully generated Party C")
            } else {
                logger.info("Houston, we down, \uD83D\uDCA6 status :  $status ")
            }
        } else {
            logger.info("Generating data .. \uD83D\uDCA6 but we are not generating accounts")
        }


    }
    private fun generateInvoices(index:Int, max: Int = 1) {
        var params : MutableMap<String, String> = mutableMapOf()
        params["max"] = max.toString()
        when(index) {
            0 -> {
                logger.info("\uD83D\uDE21  generateInvoices for PARTY A  \uD83D\uDE21  \uD83D\uDE21 ")
                val response = httpGet(
                        timeout = 990000000.0, params = params,
                        url = "http://localhost:10050/admin/generateInvoices")
                logger.info("\uD83C\uDF4E  RESPONSE: statusCode: ${response.statusCode}  " +
                        "${response.text}")
            }
            1 -> {

                logger.info("\uD83D\uDE21  generateInvoices for PARTY B  \uD83D\uDE21  \uD83D\uDE21 ")
                val response2 = httpGet(
                        timeout = 990000000.0, params = params,
                        url = "http://localhost:10053/admin/generateInvoices")
                logger.info("\uD83C\uDF4E RESPONSE: statusCode: ${response2.statusCode}  " +
                        "${response2.text}")
            }
            2 -> {
                logger.info("\uD83D\uDE21  generateInvoices for PARTY C  \uD83D\uDE21  \uD83D\uDE21 ")
                val response3 = httpGet(
                        timeout = 990000000.0, params = params,
                        url = "http://localhost:10056/admin/generateInvoices")
                logger.info("\uD83C\uDF4E  RESPONSE: statusCode: ${response3.statusCode}  " +
                        "${response3.text}")
            }
        }




    }
    private fun generateCrossNodeInvoices(index:Int, max: Int = 1) {
        var params : MutableMap<String, String> = mutableMapOf()
        params["max"] = max.toString()
        when(index) {
            0 -> {
                logger.info("\uD83D\uDE21  generateCrossNodeInvoices for PARTY A  \uD83D\uDE21  \uD83D\uDE21 ")
                val response = httpGet(
                        timeout = 990000000.0, params = params,
                        url = "http://localhost:10050/admin/generateCrossNodeInvoices")
                logger.info("\uD83C\uDF4E  RESPONSE: statusCode: ${response.statusCode}  " +
                        "${response.text}")
            }
            1 -> {

                logger.info("\uD83D\uDE21  generateCrossNodeInvoices for PARTY B  \uD83D\uDE21  \uD83D\uDE21 ")
                val response2 = httpGet(
                        timeout = 990000000.0, params = params,
                        url = "http://localhost:10053/admin/generateCrossNodeInvoices")
                logger.info("\uD83C\uDF4E RESPONSE: statusCode: ${response2.statusCode}  " +
                        "${response2.text}")
            }
            2 -> {
                logger.info("\uD83D\uDE21  generateCrossNodeInvoices for PARTY C  \uD83D\uDE21  \uD83D\uDE21 ")
                val response3 = httpGet(
                        timeout = 990000000.0, params = params,
                        url = "http://localhost:10056/admin/generateCrossNodeInvoices")
                logger.info("\uD83C\uDF4E  RESPONSE: statusCode: ${response3.statusCode}  " +
                        "${response3.text}")
            }
        }




    }
    private fun generateOffers(index:Int) {
        when (index) {
            0 -> {
                logger.info("\uD83D\uDE21 \uD83D\uDE21 generateOffers for PARTY A  \uD83D\uDE21  \uD83D\uDE21 ")
                val response = httpGet(
                        timeout = 990000000.0,
                        url = "http://localhost:10050/admin/generateOffers")
                logger.info("\uD83C\uDF4E  RESPONSE: statusCode: ${response.statusCode}  ${response.text}")
            }
            1 -> {
                logger.info("\uD83D\uDE21 \uD83D\uDE21 generateOffers for PARTY B  \uD83D\uDE21  \uD83D\uDE21 ")
                val response2 = httpGet(
                        timeout = 990000000.0,
                        url = "http://localhost:10053/admin/generateOffers")
                logger.info("\uD83C\uDF4E  RESPONSE: statusCode: ${response2.statusCode}   ${response2.text}")

            }
            2 -> {
                logger.info("\uD83D\uDE21 \uD83D\uDE21 generateOffers for PARTY C  \uD83D\uDE21  \uD83D\uDE21 ")
                val response3 = httpGet(
                        timeout = 990000000.0,
                        url = "http://localhost:10056/admin/generateOffers")
                logger.info("\uD83C\uDF4E RESPONSE: statusCode: ${response3.statusCode}  ${response3.text}")
            }
        }



    }
    private fun startAccountsForNode(url: String, deleteFirestore: Boolean ): Int {
        logger.info("\uD83D\uDD35 \uD83D\uDD35 \uD83D\uDD35 \uD83D\uDD35 \uD83D\uDD35 " +
                "\uD83D\uDD35 \uD83D\uDD35 generateAccounts: $url deleteFirestore: $deleteFirestore")
        val response = httpGet(
                timeout = 990000000.0,
                url = "$url/admin/demo",
                params = mapOf("deleteFirestore" to deleteFirestore.toString()))

        logger.info("\uD83C\uDF4E \uD83C\uDF4E RESPONSE: statusCode: ${response.statusCode}  " +
                "\uD83C\uDF4E \uD83C\uDF4E ${response.text}")
        return response.statusCode
    }
    private fun doNodesAndAggregates(proxyPartyA: CordaRPCOps, proxyPartyB: CordaRPCOps, proxyPartyC: CordaRPCOps, proxyReg: CordaRPCOps) {
        logger.info("\n++++++++++++++   NODES \uD83C\uDFC0 \uD83C\uDFC0 \uD83C\uDFC0 ++++++++++++++++++++++++\n")
        getNodes(proxyPartyA)
        //
        logger.info("\n++++++++++++++   PARTYA \uD83C\uDF4A ${proxyPartyA.nodeInfo().addresses.first()} " +
                " \uD83E\uDD6C ${proxyPartyA.nodeInfo().legalIdentities.first().name} \uD83C\uDFC0 \uD83C\uDFC0 \uD83C\uDFC0 ++++++++++++++++++++++++\n")
        getAggregates(proxyPartyA)
        logger.info("\n++++++++++++++   PARTYB \uD83C\uDF4A ${proxyPartyB.nodeInfo().addresses.first()}  " +
                " \uD83E\uDD6C ${proxyPartyB.nodeInfo().legalIdentities.first().name} \uD83C\uDFC0 \uD83C\uDFC0 \uD83C\uDFC0 ++++++++++++++++++++++++\n")
        getAggregates(proxyPartyB)
        logger.info("\n++++++++++++++   PARTYC \uD83C\uDF4A ${proxyPartyC.nodeInfo().addresses.first()} " +
                " \uD83E\uDD6C ${proxyPartyC.nodeInfo().legalIdentities.first().name} \uD83C\uDFC0 \uD83C\uDFC0 \uD83C\uDFC0 ++++++++++++++++++++++++\n")
        getAggregates(proxyPartyC)
        logger.info("\n++++++++++++++   REGULATOR \uD83C\uDF4A ${proxyReg.nodeInfo().addresses.first()} " +
                " \uD83E\uDD6C ${proxyReg.nodeInfo().legalIdentities.first().name} \uD83C\uDFC0 \uD83C\uDFC0 \uD83C\uDFC0 ++++++++++++++++++++++++\n")
        getAggregates(proxyReg)

        //        getFlows(proxyPartyA)
        //        getFlows(proxyPartyB)
        //        getFlows(proxyPartyC)
        //        getFlows(proxyReg)
        //        getFlows(proxyNotary)
    }
    private fun getFlows(proxy: CordaRPCOps) {
        logger.info("\n\uD83D\uDD35 \uD83D\uDD35 \uD83D\uDD35 \uD83D\uDD35  \uD83C\uDF81 " +
                "Registered flows for:  \uD83D\uDD06 ${proxy.nodeInfo().legalIdentities.first()}")
        proxy.registeredFlows().forEach() {
            logger.info(" \uD83C\uDF81 Registered flow:  \uD83D\uDD06 $it")
        }
    }
    private fun getThisNode(proxy: CordaRPCOps) {
        val me = proxy.nodeInfo();
        logger.info("\uD83E\uDD6C \uD83E\uDD6C I am connected to (p2pPort): \uD83E\uDD6C ${me.addresses.first()} - \uD83C\uDF4A - ${me.legalIdentities.first()}")
    }
    private fun getNodes(proxy: CordaRPCOps) {
        val nodes = proxy.networkMapSnapshot()
        nodes.forEach() {
            logger.info("\uD83D\uDC9A \uD83D\uDC99 \uD83D\uDC9C Node found: \uD83D\uDC9A ${it.addresses.first()}  \uD83C\uDF00 \uD83C\uDF00 ${it.legalIdentities.first()}")
        }
        logger.info("\uD83C\uDD7F️ \uD83C\uDD7F️ \uD83C\uDD7F️ Nodes: ${nodes.size}")
        val notary = proxy.notaryIdentities().first();
        logger.info("\uD83D\uDD31 \uD83D\uDD31 Notary is \uD83D\uDD31 ${notary.name}")
    }
    private fun getAggregates(proxy: CordaRPCOps) {
        val criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        val page = proxy.vaultQueryByWithPagingSpec(
                criteria = criteria, contractStateType = AccountInfo::class.java,
                paging = PageSpecification(pageNumber = 1, pageSize = 1000))
        var cnt = 0
        page.states.forEach() {
            if (it.state.data.host.toString() == proxy.nodeInfo().legalIdentities.first().toString()) {
                cnt++
            }
        }
        logger.info("Accounts on Node: ♻️ $cnt ♻️")
        //
        val pageInvoices = proxy.vaultQueryByWithPagingSpec(criteria = criteria,
                contractStateType = InvoiceState::class.java,
                paging = PageSpecification(pageNumber = 1, pageSize = 2000))
        cnt = 0
        pageInvoices.states.forEach() {
            if (it.state.data.supplierInfo.host.toString() == proxy.nodeInfo().legalIdentities.first().toString()) {
                cnt++
            }
        }
        logger.info("Invoices on Node: ♻️ $cnt♻️")

        val pageInvoiceOffers =
        proxy.vaultQueryByWithPagingSpec(
                contractStateType = InvoiceOfferState::class.java,
                criteria = criteria,
                paging = PageSpecification(
                    pageNumber = 1, pageSize = 2000))
        cnt = 0
        pageInvoiceOffers.states.forEach() {
            if (it.state.data.investor.host.toString() == proxy.nodeInfo().legalIdentities.first().toString()) {
                cnt++
            }
        }

        logger.info("InvoiceOffers on Node: ♻️ $cnt ♻️")
    }
    private fun getAccountDetails(proxy: CordaRPCOps) {
        val criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        val page = proxy.vaultQueryByCriteria(criteria = criteria, contractStateType = AccountInfo::class.java)
        var cnt = 1
        val sorted = page.states.sortedBy { it.state.data.name }
        sorted.forEach() {
            logger.info("\uD83E\uDDE9\uD83E\uDDE9 Account #$cnt \uD83E\uDDE9 ${it.state.data}")
            cnt++

        }
        logger.info("Accounts on Node: ♻️ ${page.states.size} ♻️")


    }
    private fun getInvoiceDetails(proxy: CordaRPCOps) {
        val criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        val pageInvoices = proxy.vaultQueryByCriteria(criteria = criteria, contractStateType = InvoiceState::class.java)
        var cnt = 1
        val sortedInvoices = pageInvoices.states.sortedBy { it.state.data.supplierInfo.name }
        sortedInvoices.forEach() {
            logger.info("\uD83C\uDF4A\uD83C\uDF4A Invoice #$cnt \uD83C\uDF4A R${it.state.data.amount} - ${it.state.data.supplierInfo.name}")
            cnt++
        }
        logger.info("Invoices on Node: ♻️ ${pageInvoices.states.size} ♻️")

    }
    private fun runInvoiceOfferAuction(proxy: CordaRPCOps) {
        mList.clear()
        var pageNumber = 1
        val pageSize = 1200
        val page = query(proxy, pageNumber = pageNumber, pageSize = pageSize)
        addToList(page)

        val remainder: Int = (page.totalStatesAvailable % pageSize).toInt()
        var pageCnt: Int = (page.totalStatesAvailable / pageSize).toInt()
        if (remainder > 0) pageCnt++

        if (pageCnt > 1)  {
            while (pageNumber < pageCnt) {
                pageNumber++
                val pageX = query(proxy,pageNumber, pageSize)
                logger.info("................... \uD83D\uDCCC .......... \uD83D\uDCCC \uD83D\uDCCC Printing page $pageNumber")
                addToList(pageX)
            }
        }

        val sorted = mList.sortedBy { it.invoiceId.toString() }
        var cnt = 1
        logger.info("\n\n................... \uD83D\uDCCC \uD83D\uDCCC " +
                "Printing offers sorted by invoiceId .... \uD83D\uDCCC \uD83D\uDCCC")
        sorted.forEach() {
            logger.info(" \uD83D\uDD06 #$cnt supplier: ${it.supplier.name}" +
                    "host: ${it.supplier.host} \uD83C\uDF88 investor: ${it.investor.name} " +
                    " \uD83E\uDDE9 host: ${it.investor.host} - \uD83E\uDDA0 offerAmt: ${it.offerAmount} from ${it.originalAmount}")
            cnt++
        }
        logger.info("\n\nInvoiceOffers on Node: ♻️ ${page.totalStatesAvailable} ♻️")
        logger.info("InvoiceOffers gathered: ♻️ ${mList.size} ♻️")
        selectBestOffers()

    }
    private fun selectBestOffers() {
        val map: MutableMap<String,InvoiceOfferState> = mutableMapOf()
        mList.forEach() {
            map[it.invoiceId.toString()] = it
        }

        var cnt = 1
        map.forEach() {
            logger.info("\uD83C\uDF88 \uD83C\uDF88 Invoice to be processed: #$cnt " +
                    "\uD83D\uDC9A supplier: ${it.value.supplier.name} ${it.value.supplier.host} " +
                    "\uD83D\uDE21 \uD83D\uDE21 customer: ${it.value.customer.name} - ${it.value.customer.host}")
            val params: MutableMap<String, String> = mutableMapOf()
            params["accountId"] = it.value.supplier.identifier.id.toString()
            params["invoiceId"] = it.key
            params["invoiceAmount"] = it.value.originalAmount.toString()

            if (it.value.supplier.host.name.toString().contains("PartyA")) {
                logger.warn("\uD83C\uDF88 \uD83C\uDF88 selectBestOffer using PARTY A, : " +
                        "\uD83D\uDC9A supplier: ${it.value.supplier.name} ${it.value.supplier.host} " +
                        "\uD83D\uDE21 \uD83D\uDE21 investor: ${it.value.investor.name} - ${it.value.investor.host}")
                val response = httpGet(
                        timeout = 990000000.0, params = params,
                        url = "http://localhost:10050/admin/selectBestOffer")
                val result = response.text
                if (result.contains("timestamp")) {
                      logger.error("\uD83D\uDC7F \uD83D\uDC7F \uD83D\uDC7F  ERROR : $result  \uD83D\uDC7F  \uD83D\uDC7F ")
                } else {
                    logger.info("\uD83C\uDF38 RESPONSE offer:  \uD83D\uDC2C #$cnt  \uD83C\uDF38 $result")
                }
            }
            if (it.value.supplier.host.name.toString().contains("PartyB")) {
                logger.warn("\uD83C\uDF88 \uD83C\uDF88 selectBestOffer using PARTY B : " +
                        "\uD83D\uDC9A supplier: ${it.value.supplier.name} ${it.value.supplier.host} " +
                        "\uD83D\uDE21 \uD83D\uDE21 investor: ${it.value.investor.name} - ${it.value.investor.host}")
                val response = httpGet(
                        timeout = 990000000.0, params = params,
                        url = "http://localhost:10053/admin/selectBestOffer")
                val result = response.text
                if (result.contains("timestamp")) {
                    logger.error("\uD83D\uDC7F \uD83D\uDC7F \uD83D\uDC7F  ERROR : $result  \uD83D\uDC7F  \uD83D\uDC7F ")
                } else {
                    logger.info("\uD83C\uDF38 RESPONSE offer:  \uD83D\uDC2C #$cnt  \uD83C\uDF38 $result")
                }
            }
            if (it.value.supplier.host.name.toString().contains("PartyC")) {
                logger.warn("\uD83C\uDF88 \uD83C\uDF88 selectBestOffer using PARTY C, : " +
                        "\uD83D\uDC9A supplier: ${it.value.supplier.name} ${it.value.supplier.host} " +
                        "\uD83D\uDE21 \uD83D\uDE21 investor: ${it.value.investor.name} - ${it.value.investor.host}")
                val response = httpGet(
                        timeout = 990000000.0, params = params,
                        url = "http://localhost:10056/admin/selectBestOffer")
                val result = response.text
                if (result.contains("timestamp")) {
                    logger.error("\uD83D\uDC7F \uD83D\uDC7F \uD83D\uDC7F  ERROR : $result  \uD83D\uDC7F  \uD83D\uDC7F ")
                } else {
                    logger.info("\uD83C\uDF38 RESPONSE offer:  \uD83D\uDC2C #$cnt  \uD83C\uDF38 $result")
                }
            }
            cnt++
        }
    }
    private val mList: MutableList<InvoiceOfferState> = mutableListOf()
    private fun addToList(page: Vault.Page<InvoiceOfferState>) {

        var cnt = 1
        page.states.forEach() {
            mList.add(it.state.data)
            cnt++
        }
    }
    private fun query(proxy: CordaRPCOps, pageNumber: Int, pageSize: Int): Vault.Page<InvoiceOfferState> {
        val criteria = QueryCriteria.VaultQueryCriteria(
                status = Vault.StateStatus.UNCONSUMED)

        return  proxy.vaultQueryByWithPagingSpec(
                contractStateType = InvoiceOfferState::class.java,
                paging = PageSpecification(pageNumber = pageNumber, pageSize = pageSize),
                criteria = criteria)
    }
}
