package com.bfn.flows.invoices

import co.paralleluniverse.fibers.Suspendable
import com.bfn.flows.regulator.BroadcastTransactionFlow
import com.bfn.flows.regulator.ReportToRegulatorFlow
import com.google.common.collect.ImmutableList
import com.template.contracts.InvoiceContract
import com.template.states.InvoiceState
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

@InitiatingFlow
@StartableByRPC
class InvoiceRegistrationFlow(private val invoiceState: InvoiceState) : FlowLogic<SignedTransaction?>() {
    private val SENDING_TRANSACTION = ProgressTracker.Step("Sending transaction to counterParty")
    private val GENERATING_TRANSACTION = ProgressTracker.Step("Generating transaction based on new IOU.")
    private val GENERATING_KEYS = ProgressTracker.Step("Generating transaction based on new IOU.")

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

    override val progressTracker = ProgressTracker(
            GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGNATURES,
            FINALISING_TRANSACTION,
            SENDING_TRANSACTION,
            GENERATING_KEYS
    )

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction? {
        val serviceHub = serviceHub
        Companion.logger.info(" \uD83E\uDD1F \uD83E\uDD1F  \uD83E\uDD1F \uD83E\uDD1F  ... InvoiceRegistrationFlow call started ...")
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        checkDuplicate(serviceHub)
        progressTracker.currentStep = GENERATING_KEYS

        //todo - SORT OUT THIS ACCOUNT THING ---> EXAMPLES FUCKED!
        val customerParty = invoiceState.customerInfo.host //subFlow(RequestKeyForAccount(customerAccount!!))
        logger.info(" \uD83E\uDD4F  \uD83E\uDD4F  \uD83E\uDD4F customerAccount: ${invoiceState.customerInfo}")
        logger.info(" \uD83E\uDD4F  \uD83E\uDD4F  \uD83E\uDD4F customerParty: $customerParty")

        //val supplierAccount = accountService.accountInfo(invoiceState.supplierInfo.identifier.id)?.state?.data
        val supplierParty = invoiceState.supplierInfo.host //subFlow(RequestKeyForAccount(supplierAccount!!))

        logger.info("\uD83C\uDF4F \uD83C\uDF4F \uD83C\uDF4F supplierAccount: ${invoiceState.supplierInfo}")
        logger.info("\uD83C\uDF4F \uD83C\uDF4F \uD83C\uDF4F supplierParty: $supplierParty")

        val customerOrg = invoiceState.customerInfo.host.name.organisation
        val supplierOrg = invoiceState.supplierInfo.host.name.organisation
        val command = InvoiceContract.Register()
        progressTracker.currentStep = GENERATING_TRANSACTION

        val txBuilder = TransactionBuilder(notary)
                .addOutputState(invoiceState, InvoiceContract.ID)
                .addCommand(
                        command,
                        supplierParty.owningKey,
                        customerParty.owningKey)

        progressTracker.currentStep = VERIFYING_TRANSACTION
        txBuilder.verify(serviceHub)
        progressTracker.currentStep = SIGNING_TRANSACTION
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        val nodeInfo = serviceHub.myInfo
        val thisNodeOrg = nodeInfo.legalIdentities.first().name.organisation
        val supplierStatus: Int
        val customerStatus: Int
        supplierStatus = if (supplierOrg.equals(thisNodeOrg, ignoreCase = true)) {
            LOCAL_SUPPLIER
        } else {
            REMOTE_SUPPLIER
        }
        customerStatus = if (customerOrg.equals(thisNodeOrg, ignoreCase = true)) {
            LOCAL_CUSTOMER
        } else {
            REMOTE_CUSTOMER
        }

        if (supplierStatus == LOCAL_SUPPLIER && customerStatus == LOCAL_CUSTOMER) {
            Companion.logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 Supplier and Customer are on the same node ..." +
                    " \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21")
            val mSignedTransactionDone = subFlow(
                    FinalityFlow(signedTx, ImmutableList.of<FlowSession>()))
            Companion.logger.info("\uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D  SAME NODE ==> FinalityFlow has been executed " +
                    "... \uD83E\uDD66 \uD83E\uDD66")
            broadcastInvoice(mSignedTransactionDone)
            //reportToRegulator(mSignedTransactionDone)
            return mSignedTransactionDone
        }

        return processRemoteNode(supplierStatus, customerStatus, customerParty, signedTx, supplierParty)
    }

    @Suspendable
    private fun processRemoteNode(supplierStatus: Int, customerStatus: Int, customerParty: Party, signedTx: SignedTransaction, supplierParty: Party): SignedTransaction? {
        Companion.logger.info("\uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 Supplier and Customer are NOT on the same node ..." +
                "  \uD83D\uDE21 flowSession(s) required ... \uD83D\uDE21")
        var supplierSession: FlowSession
        var customerSession: FlowSession
        var signedTransaction: SignedTransaction? = null
        if (supplierStatus == LOCAL_SUPPLIER && customerStatus == REMOTE_CUSTOMER) {
            Companion.logger.info("\uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 LOCAL_SUPPLIER and REMOTE_CUSTOMER \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21")
            customerSession = initiateFlow(customerParty)
            signedTransaction = getSignedTransaction(signedTx, ImmutableList.of(customerSession))
        }
        if (supplierStatus == REMOTE_SUPPLIER && customerStatus == LOCAL_CUSTOMER) {
            Companion.logger.info("\uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 REMOTE_SUPPLIER and LOCAL_CUSTOMER \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21")
            supplierSession = initiateFlow(supplierParty)
            signedTransaction = getSignedTransaction(signedTx, ImmutableList.of(supplierSession))
        }

        if (supplierStatus == REMOTE_SUPPLIER && customerStatus == REMOTE_CUSTOMER) {
            Companion.logger.info("\uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 REMOTE_SUPPLIER and REMOTE_CUSTOMER  \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21")
            supplierSession = initiateFlow(supplierParty)
            if (invoiceState.supplierInfo.host.name != invoiceState.customerInfo.host.name) {
                customerSession = initiateFlow(customerParty)
                signedTransaction = getSignedTransaction(signedTx, ImmutableList.of(supplierSession, customerSession))
            } else {
                signedTransaction = getSignedTransaction(signedTx, ImmutableList.of(supplierSession))
            }
        }
        if (signedTransaction != null) {
            broadcastInvoice(signedTransaction)
            //reportToRegulator(signedTransaction)
        }
        return signedTransaction
    }

    @Suspendable
    @Throws(FlowException::class)
    private fun reportToRegulator(mSignedTransactionDone: SignedTransaction) {
        try {
            subFlow(ReportToRegulatorFlow(mSignedTransactionDone))
        } catch (e: Exception) {
            Companion.logger.error(" \uD83D\uDC7F  \uD83D\uDC7F  \uD83D\uDC7F Regulator fell down.  \uD83D\uDC7F IGNORED  \uD83D\uDC7F ", e)
            throw FlowException("Regulator fell down!")
        }
    }
    @Suspendable
    @Throws(FlowException::class)
    private fun broadcastInvoice(mSignedTransactionDone: SignedTransaction) {
        try {
            subFlow(BroadcastTransactionFlow(mSignedTransactionDone))
        } catch (e: Exception) {
            Companion.logger.error("\uD83D\uDC7F \uD83D\uDC7F \uD83D\uDC7F BroadcastTransactionFlow fell down.  \uD83D\uDC7F IGNORED  \uD83D\uDC7F ", e)
            throw FlowException("BroadcastTransactionFlow fell down!", e)
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
        val mSignedTransactionDone = subFlow(
                FinalityFlow(signedTransaction, sessions))
        Companion.logger.info("\uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D  " +
                " \uD83D\uDC4C \uD83D\uDC4C \uD83D\uDC4C OTHER NODE(S): FinalityFlow has been executed ..." +
                "\uD83C\uDF40 \uD83C\uDF40 \uD83C\uDF40 YEBO!! " +
                "\uD83E\uDD66 \uD83E\uDD66")
        return mSignedTransactionDone
    }

    @Suspendable
    @Throws(FlowException::class)
    private fun checkDuplicate(serviceHub: ServiceHub) {
        val criteria = VaultQueryCriteria(StateStatus.UNCONSUMED)
        val (refs) = serviceHub.vaultService.queryBy(InvoiceState::class.java, criteria,
                PageSpecification(1, 200))
        var isFound = false
        Companion.logger.info(" \uD83D\uDCA6  \uD83D\uDCA6 Number of InvoiceStates:  \uD83D\uDCA6 " + refs.size + "  \uD83D\uDCA6")
        for ((state1) in refs) {
            val state = state1.data
            if (invoiceState.invoiceNumber
                            .equals(state.invoiceNumber, ignoreCase = true)
                    && invoiceState.supplierInfo.identifier.id.toString()
                            .equals(state.supplierInfo.identifier.id.toString(), ignoreCase = true)) {
                isFound = true
            }
        }
        if (isFound) {
            throw FlowException("InvoiceState is already on file")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(InvoiceRegistrationFlow::class.java)
        private const val LOCAL_SUPPLIER = 1
        private const val LOCAL_CUSTOMER = 2
        private const val REMOTE_SUPPLIER = 3
        private const val REMOTE_CUSTOMER = 4
    }

}
