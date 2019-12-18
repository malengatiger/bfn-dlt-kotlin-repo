package com.bfn.flows.invoices

import co.paralleluniverse.fibers.Suspendable

import com.bfn.flows.regulator.ReportToRegulatorFlow
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
import com.template.contracts.OfferAndTokenStateContract
import com.template.states.InvoiceOfferState
import com.template.states.OfferAndTokenState
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
import java.security.PublicKey
import java.util.*
import kotlin.collections.ArrayList

/**
 * Supplier selects best offer from investors and issue tokens for winning offer
 */
@InitiatingFlow
@StartableByRPC
class BestOfferForInvoiceFlow(private val supplierAccountId: String,
                              private val invoiceId: String) : FlowLogic<OfferAndTokenState>() {

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): OfferAndTokenState {
        Companion.logger.info(" \uD83E\uDD1F \uD83E\uDD1F  \uD83E\uDD1F \uD83E\uDD1F  " +
                "... \uD83D\uDE3C \uD83D\uDE3C BestOfferForInvoiceFlow call started ... \uD83D\uDE3C \uD83D\uDE3C ")
        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
        val supplierAccount = accountService.accountInfo(UUID.fromString(supplierAccountId))!!.state.data
        Companion.logger.info(" \uD83C\uDF00 \uD83C\uDF00 ${supplierAccount.name} selecting best offer for invoice: \uD83D\uDE3C $invoiceId...")
        val oat = checkTokens()
        if (oat != null) {
            logger.info("\uD83D\uDECE \uD83D\uDECE Returning existing token: \uD83D\uDECE ${oat.token}")
            return oat
        }
        val (list: MutableList<StateAndRef<InvoiceOfferState>>, selected) = filterOffersByParams()
        //issue tokens
        val token: FungibleToken = createToken(selected.state.data)
        //create tx to share token with holder
        Companion.logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 " +
                "creating transactionBuilder with token  \uD83C\uDF38 $token ...  \uD83D\uDD06")

        val tokenCommand = IssueTokenCommand(token = token.issuedTokenType, outputs = listOf(0))
        val offerAndTokenCmd = OfferAndTokenStateContract.CreateOfferAndToken()

        val publicKeys : MutableList<PublicKey> = mutableListOf()
        val parties : MutableList<Party> = mutableListOf()
        publicKeys.add(serviceHub.ourIdentity.owningKey)
        if (selected.state.data.supplier.host.name.toString() != serviceHub.ourIdentity.name.toString()) {
            publicKeys.add(selected.state.data.supplier.host.owningKey)
            parties.add(selected.state.data.supplier.host)
        }
        if (selected.state.data.investor.host.name.toString() != serviceHub.ourIdentity.name.toString()) {
            publicKeys.add(selected.state.data.investor.host.owningKey)
            parties.add(selected.state.data.investor.host)
        }
        val offerAndToken = OfferAndTokenState(selected.state.data,token,serviceHub.ourIdentity)

        val tx = buildAndVerifyTransactions(tokenCommand, offerAndTokenCmd, token,
                offerAndToken, list, publicKeys, parties)

        Companion.logger.info("\uD83C\uDF6F \uD83C\uDF6F Yebo! OfferAndToken sorted out!!!: ${selected.state.data.offerAmount} " +
                " supplier : ${selected.state.data.supplier.name}  ${selected.state.data.supplier.host} \uD83D\uDC4C " +
                " investor: ${selected.state.data.investor.name} ${selected.state.data.investor.host} \uD83E\uDDE9 " +
                "Token issued:  \uD83C\uDF40 $token \uD83E\uDDE9 txId: ${tx.id}")

        return offerAndToken
    }

    @Suspendable
    private fun buildAndVerifyTransactions(
            tokenCommand: IssueTokenCommand,
            offerAndTokenCmd: OfferAndTokenStateContract.CreateOfferAndToken,
            token: FungibleToken,
            offerAndToken: OfferAndTokenState,
            offers: MutableList<StateAndRef<InvoiceOfferState>>,
            publicKeys: List<PublicKey>,
            parties: List<Party>): SignedTransaction {
        Companion.logger.info("\uD83D\uDC7A \uD83D\uDC7A buildAndVerifyTransactions: Offers to consume: ${offers.size} " +
                "to be added to transaction \uD83D\uDC7A \uD83D\uDC7A")

        val transactionBuilderToken = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.first())

        transactionBuilderToken
                .addCommand(tokenCommand, publicKeys)
                .addCommand(offerAndTokenCmd, publicKeys)
                .addOutputState(token)
                .addOutputState(offerAndToken)
        //verify and sign
        Companion.logger.info("\uD83D\uDE3C \uD83D\uDE3C \uD83D\uDE3C \uD83D\uDE3C \uD83D\uDE3C \uD83D\uDE3C Verify transaction")
        transactionBuilderToken.verify(serviceHub)

        Companion.logger.info("\uD83D\uDE3C \uD83D\uDE3C \uD83D\uDE3C \uD83D\uDE3C \uD83D\uDE3C \uD83D\uDE3C signInitialTransaction ")
        val signedTokenTx = serviceHub.signInitialTransaction(transactionBuilderToken)

        Companion.logger.info("\uD83D\uDE3C \uD83D\uDE3C \uD83D\uDE3C \uD83D\uDE3C \uD83D\uDE3C \uD83D\uDE3C Start InvoiceCloseFlow")
        subFlow(InvoiceCloseFlow(invoiceId = invoiceId))

        Companion.logger.info("\uD83D\uDE3C \uD83D\uDE3C \uD83D\uDE3C \uD83D\uDE3C \uD83D\uDE3C \uD83D\uDE3C Start InvoiceOfferCloseFlow")
        subFlow(InvoiceOfferCloseFlow(offers))

        logger.warn("\uD83D\uDC38  \uD83D\uDC38  \uD83D\uDC38  \uD83D\uDC38  \uD83D\uDC38 Finally ready to finish this ...")
        return finalizeToken(parties, signedTokenTx)

    }

    @Suspendable
    private fun finalizeToken(parties: List<Party>,
                              partlySignedTransaction: SignedTransaction): SignedTransaction {

        Companion.logger.info("\uD83D\uDC33 \uD83D\uDC33 \uD83D\uDC33 \uD83D\uDC33 finalizeToken ... ${parties.size} parties")
        val flowSessions: MutableList<FlowSession> = mutableListOf()
        parties.forEach() {
            if (it.toString() != serviceHub.ourIdentity.toString()) {
                flowSessions.add(initiateFlow(it))
            }
        }
        var signedTransaction: SignedTransaction?
        if (flowSessions.isNotEmpty()) {
            signedTransaction = collectSignatures(partlySignedTransaction, flowSessions)
            signedTransaction = subFlow(FinalityFlow(signedTransaction, listOf()))
            Companion.logger.info("\uD83D\uDC7A \uD83D\uDECE \uD83D\uDECE \uD83D\uDECE  " +
                    "${flowSessions.size} flowSessions (NODES INVOLVED) ==> ️Transaction completed OK:" +
                    "  \uD83D\uDECE  \uD83D\uDECE  \uD83D\uDECE YEBO!")
        } else {
            signedTransaction = subFlow(FinalityFlow(partlySignedTransaction, listOf()))
            Companion.logger.info("\uD83D\uDC7A \uD83D\uDECE \uD83D\uDECE \uD83D\uDECE  " +
                    "only LOCAL NODE INVOLVED ==> ️Transaction completed OK:" +
                    "  \uD83D\uDECE \uD83D\uDECE \uD83D\uDECE YEBO!")
        }


        return signedTransaction
    }

    @Suspendable
    private fun checkTokens(): OfferAndTokenState? {
        var isFound = false
        val page = serviceHub.vaultService.queryBy(
                criteria = VaultQueryCriteria(status = StateStatus.UNCONSUMED),
                paging = PageSpecification(
                        pageSize = 500,
                        pageNumber = 1),
                contractStateType = OfferAndTokenState::class.java)

        var offerAndToken: OfferAndTokenState? = null
        page.states.forEach() {
            val mId = it.state.data.invoiceOffer.invoiceId.toString()
            if (mId == invoiceId) {
                isFound = true
                offerAndToken = it.state.data
            }
        }
        if (isFound) {
            val msg = "\uD83D\uDC7F This invoice has already been taken. \uD83D\uDC7F \uD83D\uDC7F Sorry Senor!"
            Companion.logger.error(msg)
//            throw IllegalStateException(msg)
        }
        return offerAndToken
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
        Companion.logger.info("\uD83E\uDDE9 \uD83E\uDDE9 Issuing Token: supplier: ${selected.supplier.host}  " +
                "\uD83C\uDF3F  investor: ${selected.investor.host} \uD83C\uDF3F ")

        val stateAndRef = serviceHub.accountService.accountInfo(selected.investor.identifier.id)
        val account = stateAndRef!!.state.data

        val issuer: Party = serviceHub.ourIdentity
        val zarTokenType = TokenType("ZAR", 2)
        val myIssuedTokenType: IssuedTokenType = zarTokenType issuedBy issuer

        val anonParty = subFlow(RequestKeyForAccount(account))
        val fungibleToken: FungibleToken =  BigDecimal(selected.offerAmount) of myIssuedTokenType heldBy anonParty
        Companion.logger.info("\uD83E\uDDE9 \uD83E\uDDE9 Token: ${fungibleToken.issuedTokenType.tokenType.tokenIdentifier} " +
                "created for \uD83C\uDF3F  $anonParty  \uD83C\uDF3F ")

        return fungibleToken
    }

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
        //todo - what if multiple offers have same offerAmount? Who gets the deal????
        logger.info("\uD83C\uDF4F \uD83C\uDF4F check order of amount, and check multiple largest ")
        sorted.forEach() {
            logger.info("\uD83C\uDF4F \uD83C\uDF4F investor ${it.state.data.investor.name} \uD83C\uDF4F offerAmount: ${it.state.data.offerAmount} ")
        }

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
        private val logger = LoggerFactory.getLogger(BestOfferForInvoiceFlow::class.java)
    }

}
