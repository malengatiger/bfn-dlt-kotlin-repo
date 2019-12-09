package com.bfn.flows.invoices

import co.paralleluniverse.fibers.Suspendable
import com.google.common.collect.ImmutableList
import com.template.InvoiceOfferContract
import com.bfn.flows.regulator.ReportToRegulatorFlow
import com.template.states.InvoiceOfferState
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import org.slf4j.LoggerFactory
import java.util.*

@InitiatingFlow
@StartableByRPC
class InvoiceOfferFlow(invoiceOfferState: InvoiceOfferState) : FlowLogic<SignedTransaction>() {
    private val invoiceOfferState: InvoiceOfferState
    private val SENDING_TRANSACTION = ProgressTracker.Step("Sending transaction to counterParty")
    private val GENERATING_TRANSACTION = ProgressTracker.Step("Generating transaction based on new IOU.")
    private val VERIFYING_TRANSACTION = ProgressTracker.Step("Verifying contract constraints.")
    private val SIGNING_TRANSACTION = ProgressTracker.Step("Signing transaction with our private key.")
    private val GATHERING_SIGNATURES: ProgressTracker.Step = object : ProgressTracker.Step("Gathering the counterparty's signature.") {
        override fun childProgressTracker(): ProgressTracker? {
            Companion.logger.info("\uD83C\uDF3A \uD83C\uDF3A ProgressTracker childProgressTracker ...")
            return CollectSignaturesFlow.tracker()
        }
    }
    private val FINALISING_TRANSACTION: ProgressTracker.Step = object : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
        override fun childProgressTracker(): ProgressTracker? {
            return FinalityFlow.tracker()
        }
    }
    // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
// checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
// function.
    override val progressTracker = ProgressTracker(
            GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGNATURES,
            FINALISING_TRANSACTION,
            SENDING_TRANSACTION
    )

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
        val serviceHub = serviceHub
        Companion.logger.info(" \uD83E\uDD1F \uD83E\uDD1F  \uD83E\uDD1F \uD83E\uDD1F  ... InvoiceOfferFlow call started ...")
        Companion.logger.info("  invoiceOfferState: InvoiceId: " + invoiceOfferState.invoiceId.toString())
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        checkDuplicate(serviceHub)
       
        val command = InvoiceOfferContract.MakeOffer()
        Companion.logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 Notary: " + notary.name.toString()
                + "  \uD83C\uDF4A supplierParty: " + invoiceOfferState.supplier.name
                + "  \uD83C\uDF4AinvestorParty: " + invoiceOfferState.investor.name
                + " \uD83C\uDF4E  discount: " + invoiceOfferState.discount
                + "  \uD83D\uDC9A offerAmount" + invoiceOfferState.offerAmount)
        val supplierParty: Party = invoiceOfferState.supplier.host
        val investorParty: Party = invoiceOfferState.investor.host
        val customerParty: Party = invoiceOfferState.customer.host
        val investorKey = investorParty.owningKey
        val supplierKey = supplierParty.owningKey
        val customerKey = customerParty.owningKey
        progressTracker.currentStep = GENERATING_TRANSACTION
        val txBuilder = TransactionBuilder(notary)
        txBuilder.addOutputState(invoiceOfferState, InvoiceOfferContract.ID)
        txBuilder.addCommand(command, supplierKey,
                investorKey, customerKey)
        progressTracker.currentStep = VERIFYING_TRANSACTION
        txBuilder.verify(serviceHub)
        Companion.logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 Invoice Offer TransactionBuilder verified")
        // Signing the transaction.
        progressTracker.currentStep = SIGNING_TRANSACTION
        val signedTx = serviceHub.signInitialTransaction(txBuilder)
        Companion.logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 Invoice Offer Transaction signInitialTransaction executed ...")
        val nodeInfo = serviceHub.myInfo
        val investorOrg: String = invoiceOfferState.investor.host.name.organisation
        val supplierOrg: String = invoiceOfferState.supplier.host.name.organisation
        val customerOrg: String = invoiceOfferState.customer.host.name.organisation
        val thisNodeOrg = nodeInfo.legalIdentities[0].name.organisation
        val matrix = Matrix()
        matrix.supplierIsRemote = !supplierOrg.equals(thisNodeOrg, ignoreCase = true)
        matrix.investorIsRemote = !investorOrg.equals(thisNodeOrg, ignoreCase = true)
        matrix.customerIsRemote = !customerOrg.equals(thisNodeOrg, ignoreCase = true)
        if (!matrix.supplierIsRemote && !matrix.customerIsRemote && !matrix.investorIsRemote) {
            Companion.logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 All participants are LOCAL ...")
            val mSignedTransactionDone = subFlow<SignedTransaction>(
                    FinalityFlow(signedTx, ImmutableList.of<FlowSession>()))
            Companion.logger.info("\uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D  SAME NODE ==> " +
                    "FinalityFlow has been executed ... \uD83E\uDD66 \uD83E\uDD66")
            reportToRegulator(serviceHub, mSignedTransactionDone)
            return mSignedTransactionDone
        }
        Companion.logger.info(" \uD83D\uDE21  \uD83D\uDE21  \uD83D\uDE21 Supplier and Customer are NOT on the same node ..." +
                "  \uD83D\uDE21 flowSession(s) required")
        val supplierSession: FlowSession
        val investorSession: FlowSession
        val customerSession: FlowSession
        val signedTransaction: SignedTransaction
        if (matrix.supplierIsRemote && matrix.customerIsRemote && matrix.investorIsRemote) {
            Companion.logger.info(" \uD83D\uDE21  \uD83D\uDE21  \uD83D\uDE21 All participants are REMOTE")
            investorSession = initiateFlow(investorParty)
            supplierSession = initiateFlow(supplierParty)
            customerSession = initiateFlow(customerParty)
            signedTransaction = getSignedTransaction(signedTx, ImmutableList.of(investorSession, supplierSession, customerSession))
            return signedTransaction
        }
        Companion.logger.info(" \uD83D\uDE21  \uD83D\uDE21  \uD83D\uDE21 Some participants are REMOTE and some are LOCAL")
        val flowSessions: MutableList<FlowSession> = ArrayList()
        if (matrix.customerIsRemote) {
            customerSession = initiateFlow(customerParty)
            flowSessions.add(customerSession)
        }
        if (matrix.supplierIsRemote) {
            supplierSession = initiateFlow(supplierParty)
            flowSessions.add(supplierSession)
        }
        if (matrix.investorIsRemote) {
            investorSession = initiateFlow(investorParty)
            flowSessions.add(investorSession)
        }
        Companion.logger.info(" \uD83D\uDE21  \uD83D\uDE21  \uD83D\uDE21 Number of Flow Sessions for REMOTE participants: "
                + flowSessions.size
                + " - signing transactions on different nodes")
        signedTransaction = getSignedTransaction(signedTx, flowSessions)
        reportToRegulator(serviceHub, signedTransaction)
        return signedTransaction
    }

    @Suspendable
    @Throws(FlowException::class)
    private fun reportToRegulator(serviceHub: ServiceHub, mSignedTransactionDone: SignedTransaction) {
        Companion.logger.info("\uD83D\uDCCC \uD83D\uDCCC \uD83D\uDCCC  Talking to the Regulator, for compliance, Senor! .............")
        val parties = serviceHub.identityService.partiesFromName("Regulator", false)
        val regulator = parties.iterator().next()
        try {
            subFlow(ReportToRegulatorFlow(regulator, mSignedTransactionDone))
            Companion.logger.info("\uD83D\uDCCC \uD83D\uDCCC \uD83D\uDCCC  DONE talking to the Regulator, Phew!")
        } catch (e: Exception) {
            Companion.logger.error(" \uD83D\uDC7F  \uD83D\uDC7F  \uD83D\uDC7F Regulator fell down.  \uD83D\uDC7F IGNORED  \uD83D\uDC7F ", e)
            throw FlowException("Regulator fell down!")
        }
    }

    @Suspendable
    @Throws(FlowException::class)
    private fun checkDuplicate(serviceHub: ServiceHub) {
        val criteria = VaultQueryCriteria(StateStatus.UNCONSUMED)
        val (refs) = serviceHub.vaultService.queryBy(InvoiceOfferState::class.java, criteria,
                PageSpecification(1, 200))
        var isFound = false
        Companion.logger.info(" \uD83D\uDCA6  \uD83D\uDCA6 Number of InvoiceOfferStates:  \uD83D\uDCA6 " + refs.size + "  \uD83D\uDCA6")
        for ((state1) in refs) {
            val state: InvoiceOfferState = state1.data
            if (invoiceOfferState.invoiceId.toString()
                            == state.invoiceId.toString()
                    && invoiceOfferState.investor.identifier.id.toString()
                           == state.investor.identifier.id.toString()) {
                isFound = true
            }
        }
        if (isFound) {
            throw FlowException("InvoiceOfferState is already on file")
        }
    }

    @Suspendable
    @Throws(FlowException::class)
    private fun getSignedTransaction(signedTx: SignedTransaction, sessions: List<FlowSession>): SignedTransaction {
        Companion.logger.info(" \uD83D\uDE21  \uD83D\uDE21  \uD83D\uDE21 getSignedTransaction ... sessions: " + sessions.size)
        progressTracker.currentStep = GATHERING_SIGNATURES
        Companion.logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 ... Collecting Signatures ....")
        val signedTransaction = subFlow(
                CollectSignaturesFlow(signedTx,
                        sessions,
                        GATHERING_SIGNATURES.childProgressTracker()!!))
        Companion.logger.info("\uD83C\uDFBD \uD83C\uDFBD \uD83C\uDFBD \uD83C\uDFBD  Signatures collected OK!  \uD83D\uDE21 \uD83D\uDE21 " +
                ".... will call FinalityFlow ... \uD83C\uDF3A \uD83C\uDF3A txId: "
                + signedTransaction.id.toString())
        val mSignedTransactionDone = subFlow<SignedTransaction>(
                FinalityFlow(signedTransaction, sessions))
        Companion.logger.info("\uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D  " +
                " \uD83D\uDC4C \uD83D\uDC4C \uD83D\uDC4C MULTIPLE NODE(S): FinalityFlow has been executed ... " +
                "\uD83E\uDD66 \uD83E\uDD66")
        return mSignedTransactionDone
    }

    private inner class Matrix {
        var supplierIsRemote = false
        var customerIsRemote = false
        var investorIsRemote = false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(InvoiceOfferFlow::class.java)
        private const val LOCAL_SUPPLIER = 1
        private const val LOCAL_INVESTOR = 2
        private const val REMOTE_SUPPLIER = 3
        private const val REMOTE_INVESTOR = 4
    }

    init {
        this.invoiceOfferState = invoiceOfferState
        Companion.logger.info(("\uD83C\uDF3A \uD83C\uDF3A InvoiceOfferFlow constructor with invoiceOfferState supplier: \uD83C\uDF4F "
                + invoiceOfferState.supplier.toString()) + "\n investor: " + invoiceOfferState.investor.toString())
    }
}
