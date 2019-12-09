package com.template.webserver

import com.google.gson.GsonBuilder
import com.template.dto.*
import com.template.webserver.FirebaseUtil.deleteCollections
import com.template.webserver.FirebaseUtil.deleteUsers
import com.template.webserver.FirebaseUtil.users
import com.template.webserver.WorkerBee.getAccounts
import com.template.webserver.WorkerBee.getInvoiceOfferStates
import com.template.webserver.WorkerBee.getInvoiceStates
import com.template.webserver.WorkerBee.listFirestoreNodes
import com.template.webserver.WorkerBee.listFlows
import com.template.webserver.WorkerBee.startAccountRegistrationFlow
import com.template.webserver.WorkerBee.startInvoiceOfferFlow
import com.template.webserver.WorkerBee.startInvoiceRegistrationFlow
import com.template.webserver.WorkerBee.writeNodesToFirestore
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.NodeInfo
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

object DemoUtil {
    private val logger = LoggerFactory.getLogger(DemoUtil::class.java)
    private val GSON = GsonBuilder().setPrettyPrinting().create()
    private var proxy: CordaRPCOps? = null
    private var suppliers: MutableList<AccountInfoDTO>? = null
    private var customers: MutableList<AccountInfoDTO>? = null
    private var investors: MutableList<AccountInfoDTO>? = null
    private val demoSummary = DemoSummary()
    private var myNode: NodeInfo? = null
    @Throws(Exception::class)
    fun generateLocalNodeData(mProxy: CordaRPCOps?,
                              deleteFirestore: Boolean): DemoSummary {
        proxy = mProxy
        logger.info("\n\uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 DemoUtil started ...  \uD83D\uDD06 \uD83D\uDD06 will list network components: 🧩🧩 deleteFirestore: $deleteFirestore")
        myNode = proxy!!.nodeInfo()
        if (myNode!!.legalIdentities[0].name.organisation.contains("Notary")) {
            throw Exception("Cannot add demo data to Notary")
        }
        if (myNode!!.legalIdentities[0].name.organisation.contains("Regulator")) {
            throw Exception("Cannot add demo data to Regulator")
        }
        suppliers = ArrayList()
        customers = ArrayList()
        investors = ArrayList()
        //delete Firestore data
        logger.info("\n\n👽 👽 👽 👽 deleteFirestore: $deleteFirestore\n\n")
        if (deleteFirestore) {
            try {
                deleteUsers()
                deleteCollections()
                logger.info(" 👽 👽 👽 👽 deleteFirestore: $deleteFirestore 🧩🧩 Firebase cleanUp complete")
            } catch (e: Exception) {
                e.printStackTrace()
                logger.warn("Firebase shit bombed")
                throw e
            }
        } else {
            logger.warn("🧩🧩🧩🧩🧩🧩🧩🧩🧩 deleteFirestore is 🧩 FALSE 🧩🧩🧩🧩🧩 ")
        }
        //
        logger.info(" 👽 👽 👽 👽 start data generation:  👽 👽 👽 👽  ")
        registerSupplierAccounts()
        registerCustomerAccounts()
        registerInvestorAccounts()
        //
        registerInvoices()
        //
        val list = getAccounts(proxy!!)
        var cnt = 0
        logger.info(" \uD83C\uDF4E  \uD83C\uDF4E Total Number of Accounts on Node after sharing:" +
                " \uD83C\uDF4E  \uD83C\uDF4E " + list.size)
        val userRecords = users
        for (userRecord in userRecords) {
            cnt++
            logger.info("🔵 🔵 userRecord 😡 #" + cnt + " - " + userRecord.displayName + " 😡 " + userRecord.email)
        }
        demoSummary.numberOfAccounts = list.size
        return demoSummary
    }

    var nodes: List<NodeInfoDTO>? = null
    var accounts: List<AccountInfoDTO>? = null
    @Throws(Exception::class)
    fun startNodes(mProxy: CordaRPCOps, env: Environment?): DemoSummary {
        proxy = mProxy
        val start = System.currentTimeMillis()
        demoSummary.started = Date().toString()
        nodes = listFirestoreNodes()
        if (nodes!!.isEmpty()) {
            nodes = writeNodesToFirestore(proxy!!, env!!)
        }
        demoSummary.numberOfNodes = nodes!!.size
        val flows: List<*> = listFlows(proxy!!)
        demoSummary.numberOfFlows = flows.size
        logger.info(" \uD83C\uDF4E  \uD83C\uDF4E " + nodes!!.size
                + " BFN Nodes")
        logger.info(" \uD83C\uDF4E  \uD83C\uDF4E " + flows.size
                + " BFN Flows")
        generateLocalNodeData(proxy, true)
        val nodeInfo = mProxy.nodeInfo()
        var cnt = 0
        for (dto in nodes!!) {
            val name = dto.addresses!![0]
            if (nodeInfo.legalIdentities[0].name.toString()
                            .equals(name, ignoreCase = true)) {
                logger.info("\n\uD83C\uDF36 \uD83C\uDF36 Ignoring Local Node - no data to generate")
                continue
            }
            if (dto.addresses!![0].contains("Notary")) {
                logger.info("\n\uD83C\uDF36 \uD83C\uDF36 Ignoring Notary Node - no data to generate")
                continue
            }
            if (dto.addresses!![0].contains("Regulator")) {
                logger.info("\n\uD83C\uDF36 \uD83C\uDF36 Ignoring Regulator Node - no data to generate")
                continue
            }
            try {
                executeForeignNodeDemoData(dto)
                cnt++
            } catch (e: Exception) {
                logger.error(" \uD83D\uDC7F  \uD83D\uDC7F  \uD83D\uDC7F Foreign demo data failed", e)
            }
        }
        val end = System.currentTimeMillis()
        demoSummary.ended = Date().toString()
        demoSummary.elapsedSeconds = ((end - start) / 1000).toDouble()
        demoSummary.dashboardData = regulatorDashboard
        logger.info("\uD83C\uDF81 \uD83C\uDF81 Foreign Nodes Demo Data Generated; NODES: \uD83D\uDC99 $cnt \uD83D\uDC99 ")
        return demoSummary
    }

    @get:Throws(Exception::class)
    private val regulatorDashboard: DashboardData
        private get() {
            var node: NodeInfoDTO? = null
            for (x in nodes!!) {
                if (x.addresses!![0].contains("Regulator")) {
                    node = x
                    break
                }
            }
            if (node == null) {
                throw Exception("Regulator not found")
            }
            val nodeUrl = node.webAPIUrl + "admin/getDashboardData"
            val con = callNode(nodeUrl)
            var summary: DashboardData
            BufferedReader(InputStreamReader(con.inputStream, "utf-8")).use { br ->
                val response = StringBuilder()
                var responseLine: String = ""
                while (br.readLine().also { responseLine = it } != null) {
                    response.append(responseLine.trim ())
                }
                summary = GSON.fromJson(response.toString(), DashboardData::class.java)
                logger.info("\uD83E\uDD1F \uD83E\uDD1F \uD83E\uDD1F " +
                        "Response from Regulator: \uD83E\uDD1F SUMMARY:: " + node.addresses!![0] + " \uD83E\uDD1F "
                        + GSON.toJson(summary) + "\n\n")
                return summary
            }
        }

    private fun sendOfferToOtherNodes(offer: InvoiceOfferDTO, account: AccountInfoDTO) {
        logger.info("\uD83C\uDF3C \uD83C\uDF3C \uD83C\uDF3C ️.... sendOfferToOtherNodes " + account.name + " invoiceId: " + offer.invoiceId + " amt: " + "" + offer.offerAmount)
        accounts = getAccounts(proxy!!)
        for (node in nodes!!) {
            if (node.addresses!![0].contains(account.host!!)) {
                continue
            }
            if (node.addresses!![0].contains("Regulator")) {
                continue
            }
            if (node.addresses!![0].contains("Notary")) {
                continue
            }
            logger.info("\uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D Processing accounts on Node: "
                    + node.addresses + " " + node.webAPIUrl)
            //get accounts for just this node
            val list: MutableList<AccountInfoDTO> = ArrayList()
            for (m in accounts!!) {
                if (!m.host.equals(account.host, ignoreCase = true)) {
                    list.add(m)
                }
            }
            for (m in list) {
                if (m.identifier.equals(offer.investor!!.identifier, ignoreCase = true)) {
                    continue
                }
                val x = random.nextInt(1000)
                if (x > 500) {
                    continue
                }
                offer.investor = m
                offer.offerDate = Date()
                try {
                    startInvoiceOfferFlow(proxy!!, offer)
                    logger.info("\uD83C\uDF3A  \uD83C\uDF3A " + account.name + " from " +
                            account.host + (" \uD83C\uDF3C \uD83C\uDF3C \uD83C\uDF3C ️" +
                            "Offer sent to \uD83C\uDF4E   \uD83E\uDD4F  \uD83E\uDD4F  \uD83E\uDD4F  \uD83E\uDD4F ")
                            + m.name + " at " + m.host + "  \uD83E\uDD6C invoiceId: " + "" + offer.offerAmount)
                } catch (e: Exception) {
                    logger.error("\uD83D\uDC7F  \uD83D\uDC7F  \uD83D\uDC7F Failed to send offer to remote investor", e)
                }
            }
        }
    }

    @Throws(Exception::class)
    private fun executeForeignNodeDemoData(node: NodeInfoDTO) {
        logger.info("\n\n\uD83E\uDD1F \uD83E\uDD1F \uD83E\uDD1F " +
                "Node Demo Data to Generate: " + node.webAPIUrl)
        val nodeUrl = node.webAPIUrl + "admin/demo?deleteFirestore=false"
        val con = callNode(nodeUrl)
        var summary: DemoSummary?
        BufferedReader(InputStreamReader(con.inputStream, "utf-8")).use { br ->
            val response = StringBuilder()
            var responseLine: String = ""
            while (br.readLine().also { responseLine = it } != null) {
                response.append(responseLine.trim())
            }
            summary = GSON.fromJson(response.toString(), DemoSummary::class.java)
            logger.info("\uD83E\uDD1F \uD83E\uDD1F \uD83E\uDD1F " +
                    "Response from Demo: \uD83E\uDD1F SUMMARY: NODE: " + node.addresses!![0] + " \uD83E\uDD1F "
                    + GSON.toJson(summary) + "\n\n")
        }
    }

    @Throws(Exception::class)
    private fun callNode(nodeUrl: String): HttpURLConnection {
        val url = URL(nodeUrl)
        val con = url.openConnection() as HttpURLConnection
        con.requestMethod = "GET"
        con.setRequestProperty("Content-Type", "application/json; utf-8")
        con.setRequestProperty("Accept", "*/*")
        con.doOutput = true
        val code = con.responseCode
        logger.info("\uD83E\uDD1F \uD83E\uDD1F \uD83E\uDD1F " +
                "Node Call response code: \uD83D\uDE21 " + code + " \uD83D\uDE21  - " + nodeUrl)
        if (code != 200) {
            throw Exception("Failed with status code: $code")
        }
        return con
    }

    @Throws(Exception::class)
    private fun registerSupplierAccounts() {
        logger.info("\n\n\uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 registerSupplierAccounts started ...  " +
                "\uD83D\uDD06 \uD83D\uDD06 ")
        var phone = phone
        val prefix = myNode!!.legalIdentities[0].name.organisation
        try {
            val supplier1 = startAccountRegistrationFlow(proxy!!, randomName, "$prefix$phone@gmail.com", "pass123", phone)
            suppliers!!.add(supplier1)
        } catch (e1: Exception) {
            logger.warn("Unable to add account - probable duplicate name")
        }
        phone = phone
        try {
            val supplier2 = startAccountRegistrationFlow(proxy!!, randomName, "$prefix$phone@gmail.com", "pass123", phone)
            suppliers!!.add(supplier2)
        } catch (e1: Exception) {
            logger.warn("Unable to add account - probable duplicate name")
        }
        phone = phone
        try {
            val supplier3 = startAccountRegistrationFlow(proxy!!, randomName, "$prefix$phone@gmail.com", "pass123", phone)
            suppliers!!.add(supplier3)
        } catch (e1: Exception) {
            logger.warn("Unable to add account - probable duplicate name")
        }
        phone = phone
        try {
            val supplier4 = startAccountRegistrationFlow(proxy!!, randomName, "$prefix$phone@gmail.com", "pass123", phone)
            suppliers!!.add(supplier4)
        } catch (e1: Exception) {
            logger.warn("Unable to add account - probable duplicate name")
        }
        logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 registerSupplierAccounts complete ..." +
                "  \uD83D\uDD06 \uD83D\uDD06 added " + suppliers!!.size + " accounts")
    }

    @Throws(Exception::class)
    private fun registerCustomerAccounts() {
        logger.info("\n\n\uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 " +
                "registerCustomerAccounts started ...  \uD83D\uDD06 \uD83D\uDD06 ")
        var phone = phone
        val prefix = myNode!!.legalIdentities[0].name.organisation
        try {
            val customer1 = startAccountRegistrationFlow(proxy!!, randomName, "$prefix$phone@gmail.com", "pass123", phone)
            customers!!.add(customer1)
        } catch (e1: Exception) {
            logger.warn("Unable to add account - probable duplicate name")
        }
        phone = phone
        try {
            val customer2 = startAccountRegistrationFlow(proxy!!, randomName, "$prefix$phone@gmail.com", "pass123", phone)
            customers!!.add(customer2)
        } catch (e1: Exception) {
            logger.warn("Unable to add account - probable duplicate name")
        }
        phone = phone
        try {
            val customer3 = startAccountRegistrationFlow(proxy!!, randomName, "$prefix$phone@gmail.com", "pass123", phone)
            customers!!.add(customer3)
        } catch (e1: Exception) {
            logger.warn("Unable to add account - probable duplicate name")
        }
        phone = phone
        try {
            val customer4 = startAccountRegistrationFlow(proxy!!, randomName, "$prefix$phone@gmail.com", "pass123", phone)
            customers!!.add(customer4)
        } catch (e1: Exception) {
            logger.warn("Unable to add account - probable duplicate name")
        }
        logger.info(" \uD83D\uDD06 \uD83D\uDD06 registerCustomerAccounts complete ...  " +
                "\uD83D\uDD06 \uD83D\uDD06 added " + customers!!.size + " accounts")
    }

    @Throws(Exception::class)
    private fun registerInvestorAccounts() {
        logger.info("\n\n\uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 " +
                "registerInvestorAccounts started ... " +
                " \uD83D\uDD06 \uD83D\uDD06")
        var phone = phone
        val prefix = myNode!!.legalIdentities[0].name.organisation
        try {
            val investor1 = startAccountRegistrationFlow(proxy!!, randomName, "$prefix$phone@gmail.com", "pass123", phone)
            investors!!.add(investor1)
        } catch (e1: Exception) {
            logger.warn("Unable to add account - probable duplicate name")
        }
        phone = phone
        try {
            val investor2 = startAccountRegistrationFlow(proxy!!, randomName, "$prefix$phone@gmail.com", "pass123", phone)
            investors!!.add(investor2)
        } catch (e1: Exception) {
            logger.warn("Unable to add account - probable duplicate name")
        }
        phone = phone
        try {
            val investor3 = startAccountRegistrationFlow(proxy!!, randomName, "$prefix$phone@gmail.com", "pass123", phone)
            investors!!.add(investor3)
        } catch (e1: Exception) {
            logger.warn("Unable to add account - probable duplicate name")
        }
        phone = phone
        try {
            val investor4 = startAccountRegistrationFlow(proxy!!, randomName, "$prefix$phone@gmail.com", "pass123", phone)
            investors!!.add(investor4)
        } catch (e1: Exception) {
            logger.warn("Unable to add account - probable duplicate name")
        }
        logger.info(" \uD83D\uDD06 \uD83D\uDD06 registerInvestorAccounts complete ...  " +
                "\uD83D\uDD06 \uD83D\uDD06 added " + investors!!.size + "  accounts")
    }

    private val phone: String
        private get() {
            val sb = StringBuilder()
            sb.append("27")
            sb.append(random.nextInt(9))
            sb.append(random.nextInt(9))
            sb.append(random.nextInt(9))
            sb.append(random.nextInt(9))
            sb.append(random.nextInt(9))
            sb.append(random.nextInt(9))
            sb.append(random.nextInt(9))
            sb.append(random.nextInt(9))
            sb.append(random.nextInt(9))
            return sb.toString()
        }

    private val random = Random(System.currentTimeMillis())
    @Throws(Exception::class)
    private fun registerInvoices() {
        for (supplier in suppliers!!) {
            for (customer in customers!!) {
                val m = InvoiceDTO()
                m.invoiceNumber = "INV_" + System.currentTimeMillis()
                m.supplier = supplier
                m.customer = customer
                var num = random.nextInt(500)
                if (num == 0) num = 92
                m.amount = num.toDouble()
                m.valueAddedTax = 15.0
                m.totalAmount = num * 1.15
                m.description = "Demo Invoice at " + Date().toString()
                m.dateRegistered = Date()
                val invoice = startInvoiceRegistrationFlow(proxy!!, m)
                val discount = random.nextInt(25) * 1.0
                for (investor in investors!!) {
                    try {
                        registerInvoiceOffer(invoice, supplier, investor, discount)
                    } catch (e: Exception) {
                    }
                }
            }
        }
        val invoiceStates = getInvoiceStates(proxy!!, null, false)
        logger.info(" \uD83C\uDF4A  \uD83C\uDF4A " + invoiceStates.size + " InvoiceStates on node ...  \uD83C\uDF4A ")
        demoSummary.numberOfInvoices = invoiceStates.size
        val list2 = getInvoiceOfferStates(proxy!!, null, false)
        demoSummary.numberOfInvoiceOffers = list2.size
        logger.info(" \uD83C\uDF4A  \uD83C\uDF4A " + list2.size + " InvoiceOfferStates on node ...  \uD83C\uDF4A ")
    }

    private val nodeInvoiceOffers: MutableList<InvoiceOfferDTO> = ArrayList()
    @Throws(Exception::class)
    private fun registerInvoiceOffer(invoice: InvoiceDTO, supplier: AccountInfoDTO,
                                     investor: AccountInfoDTO, discount: Double) {
        logger.info("\n\uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 registerInvoiceOffer started ..." +
                "  \uD83D\uDD06 \uD83D\uDD06 ")
        val m = InvoiceOfferDTO()
        m.invoiceId = invoice.invoiceId
        m.supplier = supplier
        m.owner = supplier
        m.investor = investor
        m.offerDate = Date()
        m.discount = discount
        if (m.discount == BigDecimal.ZERO.toDouble()) {
            m.discount = 3.5
        }
        val n = 100.0 - m.discount / 100
        m.offerAmount = n
        m.originalAmount = invoice.totalAmount
        val offer = startInvoiceOfferFlow(proxy!!, m)
        nodeInvoiceOffers.add(offer)
        sendOfferToOtherNodes(offer, supplier)
    }

    var names: MutableList<String> = ArrayList()
    var map = HashMap<String, String?>()
    @get:Throws(Exception::class)
    val randomName: String
        get() {
            names.add("Jones Pty Ltd")
            names.add("Nkosi Associates")
            names.add("Maddow Enterprises")
            names.add("Xavier Inc.")
            names.add("House Inc.")
            names.add("Washington Brookes LLC")
            names.add("Johnson Associates Pty Ltd")
            names.add("Khulula Ltd")
            names.add("Innovation Partners")
            names.add("Peach Enterprises")
            names.add("Petersen Ventures Inc")
            names.add("Nixon Associates LLC")
            names.add("NamibianCool Inc.")
            names.add("BrothersFX Inc")
            names.add("Jabula Associates Pty Ltd")
            names.add("Graystone Khambule Ltd")
            names.add("Craighall Investments Ltd")
            names.add("Robert Grayson Associates")
            names.add("KZN Wildlife Pty Ltd")
            names.add("Kumar Enterprises Ltd")
            names.add("KrugerX Steel")
            names.add("TrainServices Pros Ltd")
            names.add("Topper PanelBeaters Ltd")
            names.add("Pelosi PAC LLC")
            names.add("Blackridge Inc.")
            names.add("Soweto Engineering Works Pty Ltd")
            names.add("Soweto Bakeries Ltd")
            names.add("BlackStone Partners Ltd")
            names.add("Constitution Associates LLC")
            names.add("Gauteng Manufacturers Ltd")
            names.add("Bidenstock Pty Ltd")
            names.add("Innovation Solutions Pty Ltd")
            names.add("Schiff Ventures Ltd")
            names.add("Process Innovation Partners")
            names.add("TrendSpotter Inc.")
            names.add("KnightRider Inc.")
            names.add("Fantastica Technology Inc.")
            names.add("Flickenburg Associates Pty Ltd")
            names.add("Cyber Operations Ltd")
            names.add("WorkerBees Inc.")
            names.add("FrickerRoad LLC.")
            names.add("Mamelodi Hustlers Pty Ltd")
            names.add("Wallace Incorporated")
            names.add("Peachtree Solutions Ltd")
            names.add("InnovateSpecialists Inc")
            names.add("DealMakers Pty Ltd")
            names.add("Clarity Solutions Inc")
            names.add("UK Holdings Ltd")
            names.add("Lauraine Pty Ltd")
            names.add("Paradigm Partners Inc")
            names.add("Washington Partners LLC")
            names.add("Motion Specialists Inc")
            names.add("OpenFlights Pty Ltd")
            names.add("ProServices Pty Ltd")
            names.add("TechnoServices Inc.")
            names.add("BrokerBoy Inc.")
            names.add("GermanTree Services Ltd")
            names.add("ShiftyRules Inc")
            names.add("BrookesBrothers Inc")
            names.add("PresidentialServices Pty Ltd")
            names.add("LawBook LLC")
            names.add("CampaignTech LLC")
            names.add("Tutankhamen Ventures Ltd")
            names.add("CrookesAndTugs Inc.")
            names.add("Coolidge Enterprises Inc")
            names.add("ProGuards Pty Ltd")
            names.add("BullFinch Ventures Ltd")
            names.add("ProGears Pty Ltd")
            names.add("HoverClint Ltd")
            names.add("KrugerBuild Pty Ltd")
            names.add("Treasure Hunters Inc")
            names.add("Kilimanjaro Consultants Ltd")
            names.add("Communications Brokers Ltd")
            names.add("VisualArts Inc")
            names.add("TownshipBusiness Ltd")
            names.add("HealthServices Pty Ltd")
            names.add("Macoute Professionals Ltd")
            names.add("Melber Pro Brokers Inc")
            names.add("Bronkies Park Pty Ltd")
            names.add("WhistleBlowers Inc.")
            names.add("Charles Mignon Pty Ltd")
            names.add("IntelligenceMaker Inc.")
            names.add("CroMagnon Industries")
            names.add("Status Enterprises LLC")
            names.add("Things Inc.")
            names.add("Rainmakers Ltd")
            names.add("Forensic Labs Ltd")
            names.add("DLT TechStars Inc")
            names.add("CordaBrokers Pty Ltd")
            val name = names[random.nextInt(names.size - 1)]
            if (map.containsKey(name)) {
                throw Exception("Random name collision")
            } else {
                map[name] = name
            }
            return name
        }
}
