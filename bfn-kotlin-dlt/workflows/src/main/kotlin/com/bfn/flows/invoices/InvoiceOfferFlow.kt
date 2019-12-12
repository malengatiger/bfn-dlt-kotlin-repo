package com.bfn.flows.invoices

import co.paralleluniverse.fibers.Suspendable
import com.bfn.flows.regulator.ReportToRegulatorFlow
import com.google.common.collect.ImmutableList
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.template.InvoiceOfferContract
import com.template.states.InvoiceOfferState
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import org.slf4j.LoggerFactory


@InitiatingFlow
@StartableByRPC
class InvoiceOfferFlow(invoiceOfferState: InvoiceOfferState) : FlowLogic<SignedTransaction>() {
    private val invoiceOfferState: InvoiceOfferState = invoiceOfferState
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
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        checkDuplicate(serviceHub)
       
        val command = InvoiceOfferContract.MakeOffer()
        Companion.logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 Notary: ${notary.name}"
                + "  \uD83C\uDF4A supplierParty: ${invoiceOfferState.supplier} "
                + "  \uD83C\uDF4A investorParty: ${invoiceOfferState.investor} "
                + " \uD83C\uDF4E discount: ${invoiceOfferState.discount}"
                + "  \uD83D\uDC9A offerAmount: ${invoiceOfferState.offerAmount}")

        val supplierParty = invoiceOfferState.supplier.host
        val investorParty = invoiceOfferState.investor.host
        val customerParty = invoiceOfferState.customer.host

        progressTracker.currentStep = GENERATING_TRANSACTION
        val txBuilder = TransactionBuilder(notary)
        txBuilder.addOutputState(invoiceOfferState, InvoiceOfferContract.ID)
        txBuilder.addCommand(command, supplierParty.owningKey,
                investorParty.owningKey, customerParty.owningKey)
        progressTracker.currentStep = VERIFYING_TRANSACTION
        txBuilder.verify(serviceHub)
        Companion.logger.info("\uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 Invoice Offer TransactionBuilder verified")

        progressTracker.currentStep = SIGNING_TRANSACTION
        val signedTx = serviceHub.signInitialTransaction(txBuilder)
        Companion.logger.info("\uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 Invoice Offer Transaction signInitialTransaction executed ...")
        val nodeInfo = serviceHub.myInfo
        val investorOrg: String = invoiceOfferState.investor.host.name.organisation
        val supplierOrg: String = invoiceOfferState.supplier.host.name.organisation
        val customerOrg: String = invoiceOfferState.customer.host.name.organisation
        val thisNodeOrg = nodeInfo.legalIdentities.first().name.organisation

        val matrix = Matrix()
        matrix.supplierIsRemote = !supplierOrg.equals(thisNodeOrg, ignoreCase = true)
        matrix.investorIsRemote = !investorOrg.equals(thisNodeOrg, ignoreCase = true)
        matrix.customerIsRemote = !customerOrg.equals(thisNodeOrg, ignoreCase = true)

        if (!matrix.supplierIsRemote && !matrix.customerIsRemote && !matrix.investorIsRemote) {
            Companion.logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 " +
                    "All participants are LOCAL ... \uD83D\uDD06")
            val mSignedTransactionDone = subFlow(
                    FinalityFlow(signedTx, ImmutableList.of<FlowSession>()))
            Companion.logger.info("\uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D  SAME NODE ==> " +
                    " \uD83E\uDD66 \uD83E\uDD66  \uD83E\uDD66 \uD83E\uDD66 FinalityFlow has been executed " +
                    "...\uD83E\uDD66 \uD83E\uDD66")
            return mSignedTransactionDone
        }

        Companion.logger.info("\uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 " +
                "Supplier and/or Customer and/or Investor are NOT on the same node ..." +
                "  \uD83D\uDE21 flowSession(s) required \uD83D\uDE21")

        val supplierSession: FlowSession
        val investorSession: FlowSession
        val customerSession: FlowSession
        val signedTransaction: SignedTransaction

        if (matrix.supplierIsRemote && matrix.customerIsRemote && matrix.investorIsRemote) {
            Companion.logger.info(" \uD83D\uDE21  \uD83D\uDE21 \uD83D\uDE21 " +
                    "All participants are REMOTE \uD83D\uDE21 ")
            investorSession = initiateFlow(investorParty)
            supplierSession = initiateFlow(supplierParty)
            customerSession = initiateFlow(customerParty)
            signedTransaction = collectSignatures(signedTx, ImmutableList.of(
                    investorSession, supplierSession, customerSession))
            return signedTransaction
        }

        Companion.logger.info("\uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 " +
                "Some participants are REMOTE and some are LOCAL \uD83D\uDE21")

        val flowSessions: MutableList<FlowSession> = ArrayList()
        if (matrix.customerIsRemote) {
            val session = initiateFlow(customerParty)
            Companion.logger.info("\uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 collectSignature:" +
                    " customer: $customerParty \uD83D\uDE21 ")
            flowSessions.add(session)
        }
        if (matrix.supplierIsRemote) {
            val session = initiateFlow(supplierParty)
            Companion.logger.info("\uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 collectSignature: " +
                    "supplier: $supplierParty \uD83D\uDE21 ")
            flowSessions.add(session)
        }
        if (matrix.investorIsRemote) {
            val session = initiateFlow(investorParty)
            Companion.logger.info("\uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 collectSignature" +
                    ": investor: $investorParty \uD83D\uDE21 ")
            flowSessions.add(session)
        }
        Companion.logger.info("\uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 Collect signatures for" +
                " ${flowSessions.size} flowSessions initiated")
        signedTransaction = collectSignatures(signedTx, flowSessions)
        reportToRegulator(signedTx)
        return signedTransaction
    }

    @Suspendable
    @Throws(FlowException::class)
    private fun reportToRegulator(mSignedTransactionDone: SignedTransaction) {
        Companion.logger.info("\uD83D\uDCCC \uD83D\uDCCC \uD83D\uDCCC  Talking to the Regulator, for compliance, Senor! .............")
        try {
            subFlow(ReportToRegulatorFlow(mSignedTransactionDone))
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
    private fun collectSignatures(signedTx: SignedTransaction, sessions: List<FlowSession>): SignedTransaction {

        progressTracker.currentStep = GATHERING_SIGNATURES

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

    private inner class Matrix {
        var supplierIsRemote = false
        var customerIsRemote = false
        var investorIsRemote = false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(InvoiceOfferFlow::class.java)

    }

    init {
        Companion.logger.info(("\uD83C\uDF3A \uD83C\uDF3A InvoiceOfferFlow constructor with invoiceOfferState supplier: \uD83C\uDF4F "
                + invoiceOfferState.supplier.toString()) + "\n investor: " + invoiceOfferState.investor.toString())
    }
}
