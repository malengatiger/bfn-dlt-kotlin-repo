package com.bfn.flows.invoices

import co.paralleluniverse.fibers.Suspendable

import com.bfn.flows.regulator.ReportToRegulatorFlow
import com.google.common.collect.ImmutableList
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.corda.lib.accounts.workflows.ourIdentity
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.template.InvoiceOfferContract
import com.template.states.InvoiceOfferState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.*
import kotlin.collections.ArrayList

/**
 * Supplier selects best offer from investors and issue tokens for winning offer
 */
@InitiatingFlow
@StartableByRPC
class SelectBestInvoiceOfferFlow(private val supplierAccountId: String,
                                 private val invoiceId: String) : FlowLogic<FungibleToken>() {

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): FungibleToken {
        Companion.logger.info(" \uD83E\uDD1F \uD83E\uDD1F  \uD83E\uDD1F \uD83E\uDD1F  " +
                "... SelectBestInvoiceOfferFlow call started ...")
        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
        val supplierAccount = accountService.accountInfo(UUID.fromString(supplierAccountId))!!.state.data
        Companion.logger.info(" \uD83C\uDF00 \uD83C\uDF00 ${supplierAccount.name} selecting best invoice offer ...")

        val (list: MutableList<StateAndRef<InvoiceOfferState>>, selected) = filterOffersByParams()

        //issue tokens
        val token: FungibleToken = createToken(selected.state.data)
        //create tx to share token with holder
        Companion.logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 " +
                "creating transactionBuilder with token  \uD83C\uDF38 $token ...  \uD83D\uDD06")

        val tokenCommand = IssueTokenCommand(token = token.issuedTokenType, outputs = listOf(0))
        val transactionBuilder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.first())
        transactionBuilder
                .addCommand(tokenCommand,
                        supplierAccount.host.owningKey, selected.state.data.investor.host.owningKey)
                .addOutputState(token)

        transactionBuilder.verify(serviceHub)
        Companion.logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 " +
                "transactionBuilder VERIFIED ...  \uD83D\uDD06")
        val signedTx = serviceHub.signInitialTransaction(transactionBuilder)
        Companion.logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 " +
                "transactionBuilder signInitialTransaction executed ...  \uD83D\uDD06")
        processFlow(selected, signedTx)

        Companion.logger.info("\uD83C\uDF6F \uD83C\uDF6F Best InvoiceOffer: ${selected.state.data.offerAmount} " +
                " supplier : ${selected.state.data.supplier.name}  ${selected.state.data.supplier.host} \uD83D\uDC4C " +
                " investor: ${selected.state.data.investor.name} ${selected.state.data.investor.host} \uD83E\uDDE9 Token issued: $token \uD83E\uDDE9")

        return token
    }

    @Suspendable
    private fun processFlow(selected: StateAndRef<InvoiceOfferState>, signedTx: SignedTransaction) {
        val thisNode = serviceHub.myInfo
        if (thisNode.legalIdentities.first().toString() == selected.state.data.investor.host.toString()
                && thisNode.legalIdentities.first().toString() == selected.state.data.supplier.host.toString()) {
            Companion.logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 " +
                    "Both participants are LOCAL ... no FlowSessions required \uD83D\uDD06")
            subFlow(FinalityFlow(signedTx, ImmutableList.of<FlowSession>()))
            Companion.logger.info("\uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D  SAME NODE ==> " +
                    " \uD83E\uDD66 \uD83E\uDD66  \uD83E\uDD66 \uD83E\uDD66 FinalityFlow has been executed " +
                    "...\uD83E\uDD66 \uD83E\uDD66")
        } else {
            Companion.logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 " +
                    "One or Both participants are REMOTE ... at least one FlowSessions required \uD83D\uDD06")
            val flowSessions: MutableList<FlowSession> = ArrayList()
            if (thisNode.legalIdentities.first().toString() != selected.state.data.investor.host.toString()) {
                val investorSession = initiateFlow(selected.state.data.investor.host)
                flowSessions.add(investorSession)
            }
            if (thisNode.legalIdentities.first().toString() != selected.state.data.supplier.host.toString()) {
                val supplierSession = initiateFlow(selected.state.data.supplier.host)
                flowSessions.add(supplierSession)
            }
            collectSignatures(signedTx, flowSessions)
        }
    }
    @Suspendable
    @Throws(FlowException::class)
    private fun collectSignatures(signedTx: SignedTransaction, sessions: List<FlowSession>): SignedTransaction {

        val signedTransaction = subFlow(CollectSignaturesFlow(
                partiallySignedTx = signedTx, sessionsToCollectFrom = sessions))
        Companion.logger.info("\uD83C\uDFBD \uD83C\uDFBD \uD83C\uDFBD \uD83C\uDFBD  " +
                "Signatures collected OK!  \uD83D\uDE21 \uD83D\uDE21 " +
                ".... will call FinalityFlow ... \uD83C\uDF3A \uD83C\uDF3A txId: "
                + signedTransaction.id.toString())

        val mSignedTransactionDone = subFlow(
                FinalityFlow(signedTransaction, sessions))

        Companion.logger.info("\uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D  " +
                " \uD83D\uDC4C \uD83D\uDC4C \uD83D\uDC4C  \uD83E\uDD66 \uD83E\uDD66  " +
                "\uD83E\uDD66 \uD83E\uDD66  \uD83E\uDD66 \uD83E\uDD66 MULTIPLE NODE(S): FinalityFlow has been executed ... " +
                "\uD83E\uDD66 \uD83E\uDD66")

        return mSignedTransactionDone
    }
    @Suspendable
    private fun createToken(selected: InvoiceOfferState): FungibleToken {
        logger.info("\uD83E\uDDE9 \uD83E\uDDE9 Issuing Token: supplier: ${selected.supplier.host}  " +
                "\uD83C\uDF3F  investor: ${selected.investor.host} \uD83C\uDF3F ")

        val stateAndRef = serviceHub.accountService.accountInfo(selected.investor.identifier.id)
        val account = stateAndRef!!.state.data

        val issuer: Party = serviceHub.ourIdentity
        val zarTokenType = TokenType("ZAR", 2)
        val myIssuedTokenType: IssuedTokenType = zarTokenType issuedBy issuer

        val anonParty = subFlow(RequestKeyForAccount(account))
        val fungibleToken: FungibleToken =  BigDecimal(selected.offerAmount) of myIssuedTokenType heldBy anonParty
        logger.info("\uD83E\uDDE9 \uD83E\uDDE9 Token: ${fungibleToken.issuedTokenType.tokenType.tokenIdentifier} " +
                "created for \uD83C\uDF3F  $anonParty  \uD83C\uDF3F ")

        return fungibleToken
    }
//    @Suspendable
//    private fun closeOffersByConsumingThem(list: MutableList<StateAndRef<InvoiceOfferState>>,
//                                           selected: StateAndRef<InvoiceOfferState>) {
//        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//        val me = serviceHub.myInfo.legalIdentities.first()
//        //consume all outstanding offers
//        val flowSessions: MutableList<FlowSession> = ArrayList()
//        val command = InvoiceOfferContract.CloseOffers()
//        list.forEach() {
//            val offer = it.state.data
//            if (selected.state.data.offerAmount != offer.offerAmount) {
//                val txBuilder = TransactionBuilder(notary)
//                        .addCommand(data = command, keys = listOf(
//                                offer.supplier.host.owningKey,
//                                offer.investor.host.owningKey))
//                        .addInputState(stateAndRef = it)
//
//                val partTx = serviceHub.signInitialTransaction(txBuilder)
//                logger.info("consumeLosingOffers: \uD83C\uDF4A serviceHub.signInitialTransaction executed: " +
//                        "${offer.investor.name} ${offer.offerAmount}")
//
//                //check if supplier and investor on the same node
//                if (offer.supplier.host.name.toString() == offer.investor.host.name.toString()) {
//                    if (offer.supplier.host.name.toString() != me.name.toString()) {
//                        val session = initiateFlow(offer.supplier.host)
//                        flowSessions.add(session)
//                    }
//                } else {
//                    //check if participants are REMOTE
//                    if (offer.supplier.host.name.toString() != me.name.toString()) {
//                        val session = initiateFlow(offer.supplier.host)
//                        flowSessions.add(session)
//                    }
//                    if (offer.investor.host.name.toString() != me.name.toString()) {
//                        val session = initiateFlow(offer.investor.host)
//                        flowSessions.add(session)
//                    }
//                }
//
//            }
//        }
//        if (flowSessions.isNotEmpty()) {
//            val tx1 = subFlow(CollectSignaturesFlow(partTx, flowSessions))
//            subFlow(FinalityFlow(transaction = tx1, sessions = flowSessions))
//            Companion.logger.info(" \uD83D\uDE21 \uD83D\uDE21 Closed offers ...\uD83D\uDC99 ")
//        }
//    }
    private val pageSize:Int = 200
    @Suspendable
    fun query(pageNumber: Int): Vault.Page<InvoiceOfferState> {
        val criteria = VaultQueryCriteria(
                status = StateStatus.UNCONSUMED)

        return serviceHub.vaultService.queryBy(
                contractStateType = InvoiceOfferState::class.java,
                paging = PageSpecification(pageNumber = pageNumber, pageSize = pageSize),
                criteria = criteria)
    }
    @Suspendable
    private fun filterOffersByParams(): Pair<MutableList<StateAndRef<InvoiceOfferState>>, StateAndRef<InvoiceOfferState>> {
        val list: MutableList<StateAndRef<InvoiceOfferState>> = ArrayList()
        //get first page
        var pageNumber = 1
        val page: Vault.Page<InvoiceOfferState> = query(pageNumber)
        addToList(page = page, list = list)

        val remainder: Int = (page.totalStatesAvailable % pageSize).toInt()
        var pageCnt: Int = (page.totalStatesAvailable / pageSize).toInt()
        if (remainder > 0) pageCnt++

        if (pageCnt > 1)  {
            while (pageNumber < pageCnt) {
                pageNumber++
                val pageX = query(pageNumber)
                addToList(pageX, list)
            }
        }
        logger.info(" \uD83C\uDFC0 Offers found for the invoice:  \uD83C\uDFC0 " +
                "${list.size} offers  \uD83C\uDFC0 ")
        val sorted = list.sortedBy { it.state.data.offerAmount }
        val selected = sorted.last()

        logger.info("\uD83E\uDDE9 InvoiceOffers found for invoice:  \uD83C\uDF00 ${sorted.size}  " +
                "\uD83E\uDDE9 ${selected.state.data.offerAmount}")
        logger.info("\uD83E\uDDE9 Best InvoiceOffer found: investor: ${selected.state.data.investor.name}  " +
                "\uD83E\uDDE9 ${selected.state.data.offerAmount}")
        return Pair(list, selected)
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
