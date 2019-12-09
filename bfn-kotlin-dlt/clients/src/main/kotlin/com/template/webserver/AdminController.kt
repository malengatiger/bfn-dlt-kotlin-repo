package com.template.webserver

import com.google.firebase.auth.UserRecord
import com.google.gson.GsonBuilder
import com.template.dto.*
import com.template.webserver.WorkerBee.getAccount
import com.template.webserver.WorkerBee.getAccounts
import com.template.webserver.WorkerBee.getDashboardData
import com.template.webserver.WorkerBee.getInvoiceOfferStates
import com.template.webserver.WorkerBee.getInvoiceStates
import com.template.webserver.WorkerBee.getStates
import com.template.webserver.WorkerBee.listFlows
import com.template.webserver.WorkerBee.listNodes
import com.template.webserver.WorkerBee.listNotaries
import com.template.webserver.WorkerBee.startAccountRegistrationFlow
import com.template.webserver.WorkerBee.writeNodesToFirestore
import net.corda.core.messaging.CordaRPCOps
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.web.bind.annotation.*
import java.util.*
import java.util.concurrent.ExecutionException

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/admin") // The paths for HTTP requests are relative to this base path.
class AdminController(rpc: NodeRPCConnection) {
    val proxy: CordaRPCOps

    @Autowired
    private val env: Environment? = null

    @GetMapping(value = ["/gen"], produces = ["application/json"])
    @Throws(Exception::class)
    private fun generateData(): DemoSummary {
        if (env == null) {
            throw Exception("Environment variables not available")
        } else {
            logger.info("\uD83C\uDF40 \uD83C\uDF40 \uD83C\uDF40 " +
                    "Environment variables available \uD83C\uDF40 ")
        }
        logger.info("\n\n\uD83D\uDD35 \uD83D\uDD35 \uD83D\uDD35 starting DemoDataGenerator ... \uD83C\uDF4F")
        val result = DemoUtil.startNodes(proxy, env)
        logger.info("\n\n\uD83D\uDD35 \uD83D\uDD35 \uD83D\uDD35 DemoUtil result: " +
                " \uD83C\uDF4F " + GSON.toJson(result)
                + "    \uD83E\uDDE1 \uD83D\uDC9B \uD83D\uDC9A \uD83D\uDC99 \uD83D\uDC9C\n\n")
        return result
    }

    @GetMapping(value = ["/demo"], produces = ["application/json"])
    @Throws(Exception::class)
    private fun buildDemo(@RequestParam deleteFirestore: Boolean): DemoSummary {
        logger.info("\uD83D\uDD35 \uD83D\uDD35 \uD83D\uDD35 starting DemoDataGenerator ... \uD83C\uDF4F deleteFirestore: $deleteFirestore")
        val result = DemoUtil.generateLocalNodeData(proxy, deleteFirestore)
        logger.info("\n\n\uD83D\uDD35 \uD83D\uDD35 \uD83D\uDD35 DemoUtil result: " +
                " \uD83C\uDF4F " + GSON.toJson(result)
                + "    \uD83E\uDDE1 \uD83D\uDC9B \uD83D\uDC9A \uD83D\uDC99 \uD83D\uDC9C\n\n")
        return result
    }

    @PostMapping(value = ["/startAccountRegistrationFlow"], produces = ["application/json"])
    @Throws(Exception::class)
    private fun startAccountRegistrationFlow(@RequestBody user: UserDTO): AccountInfoDTO {
        return startAccountRegistrationFlow(proxy, user.name!!, user.email, user.password, user.cellphone)
    }

    @get:GetMapping(value = ["getAccounts"])
    val accounts: List<AccountInfoDTO>
        get() = getAccounts(proxy)

    @GetMapping(value = ["listFirestoreNodes"])
    @Throws(ExecutionException::class, InterruptedException::class)
    fun listFirestoreNodes(): List<NodeInfoDTO> {
        return WorkerBee.listFirestoreNodes()
    }

    /*
@GetMapping(value = "/states", produces = arrayOf("text/plain"))
    private fun states() = proxy.vaultQueryBy<ContractState>().states.toString()
 */
    @get:GetMapping(value = ["/getStates"], produces = ["application/json"])
    private val states: List<String>
        private get() {
            val msg = ("\uD83E\uDDE1 \uD83D\uDC9B \uD83D\uDC9A AdminController:BFN Web API pinged: " + Date().toString()
                    + " \uD83E\uDDE1 \uD83D\uDC9B \uD83D\uDC9A")
            logger.info(msg)
            return getStates(proxy)
        }

    @GetMapping(value = ["getInvoiceStates"])
    @Throws(Exception::class)
    fun getInvoiceStates(@RequestParam(value = "consumed", required = false) consumed: Boolean,
                         @RequestParam(value = "accountId", required = false) accountId: String?): List<InvoiceDTO> {
        return getInvoiceStates(proxy, accountId, consumed)
    }

    @GetMapping(value = ["getInvoiceOfferStates"])
    @Throws(Exception::class)
    fun getInvoiceOfferStates(@RequestParam(value = "consumed", required = false) consumed: Boolean,
                              @RequestParam(value = "accountId", required = false) accountId: String?): List<InvoiceOfferDTO> {
        return getInvoiceOfferStates(proxy, accountId, consumed)
    }

    @GetMapping(value = ["getUser"])
    @Throws(Exception::class)
    fun getUser(@RequestParam(value = "email", required = false) email: String?): UserRecord? {
        //        if (record == null) {
//            throw new Exception("User not found: ".concat(email));
//        }
        return FirebaseUtil.getUser(email)
    }

    @get:Throws(Exception::class)
    @get:GetMapping(value = ["getUsers"])
    val users: List<UserDTO>
        get() {
            val users: MutableList<UserDTO> = ArrayList()
            try {
                val userRecords = FirebaseUtil.users
                for (userRecord in userRecords) {
                    logger.info("🔵 🔵 userRecord 😡 " + userRecord.displayName + " 😡 " + userRecord.email)
                    val user = UserDTO()
                    user.name = userRecord.displayName
                    user.email = userRecord.email
                    user.uid = userRecord.uid
                    users.add(user)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return users
        }

    @GetMapping(value = ["getAccount"])
    @Throws(Exception::class)
    fun getAccount(@RequestParam(value = "accountId") accountId: String?): AccountInfoDTO {
        return getAccount(proxy, accountId)
    }

    @GetMapping(value = ["writeNodesToFirestore"])
    @Throws(Exception::class)
    fun writeNodesToFirestore(): List<NodeInfoDTO> {
        if (env == null) {
            throw Exception("Environment variables not available")
        } else {
            logger.info("\uD83C\uDF40 \uD83C\uDF40 \uD83C\uDF40 " +
                    "Environment variables available \uD83C\uDF40 ")
        }
        return writeNodesToFirestore(proxy, env)
    }

    @GetMapping(value = ["/hello"], produces = ["text/plain"])
    private fun hello(): String {
        logger.info("/ requested. will say hello  \uD83D\uDC9A  \uD83D\uDC9A  \uD83D\uDC9A")
        return "\uD83D\uDC9A  BFNWebApi: AdminController says  \uD83E\uDD6C HELLO WORLD!  \uD83D\uDC9A  \uD83D\uDC9A"
    }

    @GetMapping(value = ["/ping"], produces = ["application/json"])
    private fun ping(): String {
        val msg = ("\uD83E\uDDE1 \uD83D\uDC9B \uD83D\uDC9A AdminController:BFN Web API pinged: " + Date().toString()
                + " \uD83E\uDDE1 \uD83D\uDC9B \uD83D\uDC9A")
        logger.info(msg)
        val nodeInfo = proxy.nodeInfo()
        logger.info("\uD83E\uDDA0 \uD83E\uDDA0 \uD83E\uDDA0 node pinged: "
                + nodeInfo.legalIdentities[0].name.toString()
                + proxy.networkParameters.toString() + " \uD83E\uDDA0 \uD83E\uDDA0 \uD83E\uDDA0 ")
        return "\uD83C\uDF3A \uD83C\uDF3A \uD83C\uDF3A \uD83C\uDF3A  AdminController: node pinged: " +
                nodeInfo.legalIdentities[0].name.toString() +
                " \uD83C\uDF3A \uD83C\uDF3A \uD83C\uDF3A \uD83C\uDF3A " +
                proxy.networkParameters.toString()
    }

    @get:GetMapping(value = ["/getDashboardData"], produces = ["application/json"])
    private val dashboardData: DashboardData
        private get() = getDashboardData(proxy)

    @GetMapping(value = ["/nodes"], produces = ["application/json"])
    fun listNodes(): List<NodeInfoDTO> {
        return listNodes(proxy)
    }

    @GetMapping(value = ["/notaries"], produces = ["application/json"])
    private fun listNotaries(): List<String> {
        return listNotaries(proxy)
    }

    @GetMapping(value = ["/flows"], produces = ["application/json"])
    private fun listFlows(): List<String> {
        return listFlows(proxy)
    }

    private inner class PingResult internal constructor(var message: String, var nodeInfo: String)

    companion object {
        private val logger = LoggerFactory.getLogger(AdminController::class.java)
        private val GSON = GsonBuilder().setPrettyPrinting().create()
    }

    init {
        proxy = rpc.proxy
        logger.info("\uD83C\uDF3A \uD83C\uDF3A \uD83C\uDF3A AdminController: NodeRPCConnection proxy has been injected: \uD83C\uDF3A " + proxy.nodeInfo().toString())
    }
}
