package com.bfn.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService
import net.corda.core.identity.Party
import net.corda.core.node.AppServiceHub
import net.corda.core.node.NetworkParameters
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import org.slf4j.LoggerFactory

@CordaService
class BFNCordaService(private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {
    private val accountService: KeyManagementBackedAccountService
    // public api of service
    val networkParameters: NetworkParameters
        get() {
            logger.info(" \uD83D\uDC2C  \uD83D\uDC2C This node legal identity:  \uD83D\uDC2C " + serviceHub.myInfo.legalIdentities[0].name.toString())
            logger.info(" \uD83D\uDC2C  \uD83D\uDC2C NetworkParameters:  \uD83D\uDC2C " + serviceHub.networkParameters.toString())
            return serviceHub.networkParameters
        }

    @Suspendable
    @Throws(Exception::class)
    fun shareAccount(accountInfo: AccountInfo, party: Party?) {
        logger.info(" \uD83D\uDC2C  \uD83D\uDC2C shareAccount ..." + accountInfo.name)
        try {
            accountService.shareAccountInfoWithParty(accountInfo.identifier.id, party!!)
        } catch (e: Exception) {
            throw Exception("Account share failed: " + accountInfo.name, e)
        }
    }

    @Suspendable
    @Throws(Exception::class)
    fun shareAccountAcrossNodes(accountInfo: AccountInfo) {
        logger.info(" \uD83D\uDC2C \uD83D\uDC2C shareAccountsAcrossNodes ...")
        val myNode = serviceHub.myInfo
        var cnt = 0
        val nodes = serviceHub.networkMapCache.allNodes
        logger.info("\uD83D\uDD35  \uD83D\uDD35  \uD83D\uDD35 shareAccountsAcrossNodes sharing across " +
                "\uD83C\uDF4E \uD83C\uDF4E  " + nodes.size + " nodes ...")
        for (nodeInfo in nodes) {
            if (nodeInfo.legalIdentities[0].name.organisation.contains("Notary")) {
                continue
            }
            if (nodeInfo.legalIdentities[0].name.organisation
                            .equals(myNode.legalIdentities[0].name.organisation, ignoreCase = true)) {
                continue
            }
            logger.info("shareAccountsAcrossNodes: sharing \uD83C\uDF4E  \uD83C\uDF4E  " + accountInfo.name
                    + " with  \uD83C\uDF40 " + nodeInfo.legalIdentities[0].name.organisation)
            accountService.shareAccountInfoWithParty(accountInfo.identifier.id,
                    nodeInfo.legalIdentities[0])
            cnt++
            logger.info("\uD83D\uDC9A \uD83D\uDC9A \uD83D\uDC9A \uD83D\uDC9A \uD83D\uDC9A " +
                    "shareAccountsAcrossNodes account  " + accountInfo.name +
                    " \uD83C\uDF4E  \uD83C\uDF4E  #" + cnt + " shared with: "
                    + nodeInfo.legalIdentities[0].name.organisation + " \uD83E\uDD1F\uD83C\uDFFE \uD83E\uDD1F\uD83C\uDFFE")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BFNCordaService::class.java)
    }

    init {
        accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
        logger.info("\uD83E\uDD66 \uD83E\uDD66 \uD83E\uDD66 \uD83E\uDD66  BFNCordaService Constructor. \uD83D\uDCA6 " +
                "Used to distribute AccountInfo to nodes \uD83D\uDE21")

    }
}
