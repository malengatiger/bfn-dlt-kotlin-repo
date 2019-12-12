package com.bfn.flows.invoices


import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.corda.lib.accounts.workflows.ourIdentity
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.r3.corda.lib.tokens.workflows.utilities.tokenAmountWithIssuerCriteria
import com.template.states.InvoiceTokenType
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import java.math.BigDecimal
import java.util.*

@StartableByRPC
@InitiatingFlow
class IssueInvoiceTokenFlow(
        private val amount: BigDecimal,
        private val accountId: String
) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()
    @Suspendable
    override fun call(): SignedTransaction {
        logger.info(" \uD83C\uDF4E \uD83C\uDF4E IssueInvoiceTokenFlow: call started")

        val stateAndRef = serviceHub.accountService.accountInfo(accountId).single()
        val account = stateAndRef.state.data

        val issuer: Party = serviceHub.ourIdentity
        val myTokenType = TokenType("ZAR", 2)
        val myIssuedTokenType: IssuedTokenType = myTokenType issuedBy issuer

        val anonParty = subFlow(RequestKeyForAccount(account))
        val fungibleToken: FungibleToken =  amount of myIssuedTokenType heldBy anonParty
        val holderSession = initiateFlow(anonParty)

        val tx = subFlow(IssueTokensFlow(fungibleToken, listOf(holderSession)))
        logger.info(" \uD83C\uDF4E \uD83C\uDF4E Tokens issued: $fungibleToken ")
        return tx
    }
}

@StartableByRPC
@InitiatingFlow
class MoveInvoiceTokenFlow(
        private val amount: BigDecimal,
        private val accountId: String
) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()
    @Suspendable
    override fun call(): SignedTransaction {
        logger.info(" \uD83C\uDF4E \uD83C\uDF4E IssueInvoiceTokenFlow: call started")
        val stateAndRef = serviceHub.accountService.accountInfo(accountId).single()
        val account = stateAndRef.state.data

        val issuer: Party = serviceHub.ourIdentity
        val myTokenType = TokenType("ZAR", 2)
        val anonParty = subFlow(RequestKeyForAccount(account))
        val regulator = serviceHub.networkMapCache.getNodeByLegalName(
                CordaX500Name(organisation = "Regulator", locality = "London", country = "GB"))

        val result = MoveFungibleTokens(
                partyAndAmount = PartyAndAmount(anonParty, amount of myTokenType),
                observers = listOf(regulator!!.legalIdentities.first()),
                queryCriteria = tokenAmountWithIssuerCriteria(myTokenType, issuer)
        )
        logger.info(" \uD83C\uDF4E \uD83C\uDF4E Tokens moved to ${account}: result: $result ")
        return result.call()

    }
}


