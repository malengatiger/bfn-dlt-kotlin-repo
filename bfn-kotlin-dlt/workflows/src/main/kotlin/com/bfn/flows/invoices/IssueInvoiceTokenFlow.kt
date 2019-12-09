package com.bfn.flows.invoices


import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.template.states.InvoiceTokenType
import net.corda.core.contracts.Amount
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import java.math.BigDecimal
import java.util.*

@StartableByRPC
@InitiatingFlow
class IssueInvoiceTokenFlow(
        private val tokenId: String,
        private val amount: BigDecimal,
        private val accountId: String
) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()
    //  AnonymousParty anonCustomer = subFlow(new RequestKeyForAccount(invoiceState.getCustomerInfo()));
    @Suspendable
    override fun call(): SignedTransaction {
        logger.info(" \uD83C\uDF4E \uD83C\uDF4E IssueInvoiceTokenFlow: call started")

        val stateAndRef = serviceHub.accountService.accountInfo(accountId).single()
        val account = stateAndRef.state.data
        //find token
        val uuid = UUID.fromString(tokenId)
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(uuid = listOf(uuid))
        val tokenStateAndRef = serviceHub.vaultService.queryBy<InvoiceTokenType>(queryCriteria).states.single()
        val token = tokenStateAndRef.state.data.toPointer<EvolvableTokenType>()

        // Starting this flow with a new flow session.
        val anonParty = subFlow(RequestKeyForAccount(account))
        val issueTokensFlow = IssueTokens(listOf(amount of token issuedBy ourIdentity heldBy anonParty))
        val signedTx = subFlow(issueTokensFlow)

        logger.info(" \uD83C\uDF4E \uD83C\uDF4E Tokens issued: $amount of tokenId: $tokenId txId:${signedTx.id}")
        return signedTx;
    }
}

@StartableByRPC
class CreateInvoiceTokenFlow(private val invoiceId: String,
                             private val amount: Amount<TokenType>) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 IssueInvoiceOfferFlow: call started")
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val evolvableTokenType = InvoiceTokenType(invoiceId, amount, listOf(ourIdentity), linearId = UniqueIdentifier())
        val transactionState = TransactionState(evolvableTokenType, notary = notary)
        val signedTx = subFlow(CreateEvolvableTokens(transactionState))
        logger.info(evolvableTokenType.toString())
        logger.info(" \uD83C\uDF4E  \uD83C\uDF4E Token for Invoice created,  \uD83E\uDDE9 \uD83E\uDDE9 " +
                "tokenId: ${evolvableTokenType.linearId}")
        return signedTx;
    }
}
