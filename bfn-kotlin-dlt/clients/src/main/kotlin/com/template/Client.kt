package com.template
import khttp.get as httpGet
import com.google.gson.GsonBuilder
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.template.states.InvoiceOfferState
import com.template.states.InvoiceState
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.loggerFor


/**
 * Connects to a Corda node via RPC and performs RPC operations on the node.
 *
 * The RPC connection is configured using command line arguments.
 */
fun main(args: Array<String>) = Client().main(args)

private class Client {
    companion object {
        val logger = loggerFor<Client>()
        val G = GsonBuilder().setPrettyPrinting().create()
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
        startDemo(false, deleteFirestore = false);

    }
    fun startDemo(generateAccounts: Boolean = false, deleteFirestore: Boolean = false) {
        if (generateAccounts) {
            logger.info(" \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 accounts for PARTY A")
            var status = generateAccounts(url = "http://localhost:10050",
                    deleteFirestore = deleteFirestore)
            if(status == 200) {
                logger.info(" \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C Successfully generated Party A")
            } else {
                logger.info("Houston, we down, \uD83D\uDCA6 status :  $status ")
            }

            logger.info(" \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 accounts for PARTY B")
            status = generateAccounts(url = "http://localhost:10053",
                    deleteFirestore = false)
            if(status == 200) {
                logger.info(" \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C Successfully generated Party B")
            } else {
                logger.info("Houston, we down, \uD83D\uDCA6 status :  $status ")
            }
            logger.info(" \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 accounts for PARTY C")
            status = generateAccounts(url = "http://localhost:10056",
                    deleteFirestore = false)
            if(status == 200) {
                logger.info(" \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C Successfully generated Party C")
            } else {
                logger.info("Houston, we down, \uD83D\uDCA6 status :  $status ")
            }
        } else {
            logger.info("Generating data .. \uD83D\uDCA6 but we are not generating accounts")
        }

        generateInvoices()
        generateOffers()

    }
    private fun generateInvoices() {
        logger.info(" \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 invoices for PARTY A  \uD83D\uDE21  \uD83D\uDE21 ")
        val response = httpGet(
                timeout = 990000000.0,
                url = "http://localhost:10050/admin/generateInvoices")
        logger.info("\uD83C\uDF4E \uD83C\uDF4E RESPONSE: statusCode: ${response.statusCode}  " +
                "\uD83C\uDF4E \uD83C\uDF4E ${response.text}")

        logger.info(" \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 invoices for PARTY B  \uD83D\uDE21  \uD83D\uDE21 ")
        val response2 = httpGet(
                timeout = 990000000.0,
                url = "http://localhost:10053/admin/generateInvoices")
        logger.info("\uD83C\uDF4E \uD83C\uDF4E RESPONSE: statusCode: ${response2.statusCode}  " +
                "\uD83C\uDF4E \uD83C\uDF4E ${response2.text}")

        logger.info(" \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 invoices for PARTY C  \uD83D\uDE21  \uD83D\uDE21 ")
        val response3 = httpGet(
                timeout = 990000000.0,
                url = "http://localhost:10056/admin/generateInvoices")
        logger.info("\uD83C\uDF4E \uD83C\uDF4E RESPONSE: statusCode: ${response3.statusCode}  " +
                "\uD83C\uDF4E \uD83C\uDF4E ${response3.text}")
    }
    private fun generateOffers() {
        logger.info(" \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 offers for PARTY A  \uD83D\uDE21  \uD83D\uDE21 ")
        val response = httpGet(
                timeout = 990000000.0,
                url = "http://localhost:10050/admin/generateOffers")
        logger.info("\uD83C\uDF4E \uD83C\uDF4E RESPONSE: statusCode: ${response.statusCode}  " +
                "\uD83C\uDF4E \uD83C\uDF4E ${response.text}")

        logger.info(" \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 offers for PARTY B  \uD83D\uDE21  \uD83D\uDE21 ")
        val response2 = httpGet(
                timeout = 990000000.0,
                url = "http://localhost:10053/admin/generateOffers")
        logger.info("\uD83C\uDF4E \uD83C\uDF4E RESPONSE: statusCode: ${response2.statusCode}  " +
                "\uD83C\uDF4E \uD83C\uDF4E ${response2.text}")

        logger.info(" \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 offers for PARTY C  \uD83D\uDE21  \uD83D\uDE21 ")
        val response3 = httpGet(
                timeout = 990000000.0,
                url = "http://localhost:10056/admin/generateOffers")
        logger.info("\uD83C\uDF4E \uD83C\uDF4E RESPONSE: statusCode: ${response3.statusCode}  " +
                "\uD83C\uDF4E \uD83C\uDF4E ${response3.text}")
    }
    fun generateAccounts(url: String, deleteFirestore: Boolean ): Int {
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
    fun getFlows(proxy: CordaRPCOps) {
        logger.info("\n\uD83D\uDD35 \uD83D\uDD35 \uD83D\uDD35 \uD83D\uDD35  \uD83C\uDF81 " +
                "Registered flows for:  \uD83D\uDD06 ${proxy.nodeInfo().legalIdentities.first()}")
        proxy.registeredFlows().forEach() {
            logger.info(" \uD83C\uDF81 Registered flow:  \uD83D\uDD06 $it")
        }
    }
    fun getThisNode(proxy: CordaRPCOps) {
        val me = proxy.nodeInfo();
        logger.info("\uD83E\uDD6C \uD83E\uDD6C I am connected to (p2pPort): \uD83E\uDD6C ${me.addresses.first()} - \uD83C\uDF4A - ${me.legalIdentities.first()}")
    }
    fun getNodes(proxy: CordaRPCOps) {
        val nodes = proxy.networkMapSnapshot()
        nodes.forEach() {
            logger.info("\uD83D\uDC9A \uD83D\uDC99 \uD83D\uDC9C Node found: \uD83D\uDC9A ${it.addresses.first()}  \uD83C\uDF00 \uD83C\uDF00 ${it.legalIdentities.first()}")
        }
        logger.info("\uD83C\uDD7F️ \uD83C\uDD7F️ \uD83C\uDD7F️ Nodes: ${nodes.size}")
        val notary = proxy.notaryIdentities().first();
        logger.info("\uD83D\uDD31 \uD83D\uDD31 Notary is \uD83D\uDD31 ${notary.name}")
    }
    fun getAggregates(proxy: CordaRPCOps) {
        val criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        val page = proxy.vaultQueryByWithPagingSpec(criteria = criteria, contractStateType = AccountInfo::class.java,
                paging = PageSpecification(pageNumber = 1, pageSize = 200))
        logger.info("Accounts on Node: ♻️ ${page.totalStatesAvailable} ♻️")
        //
        val pageInvoices = proxy.vaultQueryByWithPagingSpec(criteria = criteria, contractStateType = InvoiceState::class.java,
                paging = PageSpecification(pageNumber = 1, pageSize = 200))
        logger.info("Invoices on Node: ♻️ ${pageInvoices.totalStatesAvailable} ♻️")

        val pageInvoiceOffers =
        proxy.vaultQueryByWithPagingSpec(contractStateType = InvoiceOfferState::class.java,
                criteria = criteria, paging = PageSpecification(pageNumber = 1, pageSize = 200))

        logger.info("InvoiceOffers on Node: ♻️ ${pageInvoiceOffers.totalStatesAvailable} ♻️")
    }
    fun getAccountDetails(proxy: CordaRPCOps) {
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
    fun getInvoiceDetails(proxy: CordaRPCOps) {
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
    fun getInvoiceOffers(proxy: CordaRPCOps) {
        val criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        val page = proxy.vaultQueryByCriteria(criteria = criteria, contractStateType = InvoiceOfferState::class.java)
        var cnt = 1
        val sorted = page.states.sortedBy { it.state.data.supplier.name }
        sorted.forEach() {
            logger.info("\uD83E\uDDE9\uD83E\uDDE9 InvoiceOffer #$cnt \uD83E\uDDE9 ${it.state.data}")
            cnt++

        }
        logger.info("InvoiceOffers on Node: ♻️ ${page.states.size} ♻️")


    }
}
