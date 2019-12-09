package com.bfn.flows.invoices

import co.paralleluniverse.fibers.Suspendable
import com.google.common.collect.ImmutableList
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.template.InvoiceContract
import com.bfn.flows.regulator.ReportToRegulatorFlow
import com.template.states.InvoiceState
import net.corda.core.flows.*
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
class InvoiceRegistrationFlow(val invoiceState: InvoiceState) : FlowLogic<SignedTransaction?>() {
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
    override fun call(): SignedTransaction? {
        val serviceHub = serviceHub
        Companion.logger.info(" \uD83E\uDD1F \uD83E\uDD1F  \uD83E\uDD1F \uD83E\uDD1F  ... InvoiceRegistrationFlow call started ...")
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        checkDuplicate(serviceHub)
        val supplierParty = invoiceState.supplierInfo.host
        val customerParty = invoiceState.customerInfo.host
        val anonCustomer = subFlow(RequestKeyForAccount(invoiceState.customerInfo))
        val anonSupplier = subFlow(RequestKeyForAccount(invoiceState.supplierInfo))
        val customerOrg = invoiceState.customerInfo.host.name.organisation
        Companion.logger.info("\uD83C\uDFC8 \uD83C\uDFC8 customerParty key: $anonCustomer")
        val supplierOrg = invoiceState.supplierInfo.host.name.organisation
        Companion.logger.info("\uD83C\uDFC8 \uD83C\uDFC8 supplierParty key: $anonSupplier")
        val command = InvoiceContract.Register()
        Companion.logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 Notary: " + notary.name.organisation
                + "  \uD83C\uDF4A supplierInfo: " + invoiceState.supplierInfo.name
                + "  \uD83C\uDF4A customerInfo: " + invoiceState.customerInfo.name + " \uD83C\uDF4E  invoice: "
                + invoiceState.invoiceNumber)
        progressTracker.currentStep = GENERATING_TRANSACTION
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(invoiceState, InvoiceContract::class.java.name)
                .addCommand(command, supplierParty.owningKey, customerParty.owningKey)
        progressTracker.currentStep = VERIFYING_TRANSACTION
        txBuilder.verify(serviceHub)
        Companion.logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 Invoice Register TransactionBuilder verified")
        progressTracker.currentStep = SIGNING_TRANSACTION
        val signedTx = serviceHub.signInitialTransaction(txBuilder)
        val nodeInfo = serviceHub.myInfo
        val thisNodeOrg = nodeInfo.legalIdentities[0].name.organisation
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
            Companion.logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 Supplier and Customer are on the same node ...")
            Companion.logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 Invoice Registration: signInitialTransaction executed ...")
            val mSignedTransactionDone = subFlow<SignedTransaction>(
                    FinalityFlow(signedTx, ImmutableList.of<FlowSession>()))
            Companion.logger.info("\uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D  SAME NODE ==> FinalityFlow has been executed ... \uD83E\uDD66 \uD83E\uDD66")
            //talk to the regulator ....
            Companion.logger.info("\uD83D\uDCCC \uD83D\uDCCC \uD83D\uDCCC  Talking to the Regulator, for compliance, Senor! .............")
            reportToRegulator(serviceHub, mSignedTransactionDone)
            return mSignedTransactionDone
        }
        Companion.logger.info(" \uD83D\uDE21  \uD83D\uDE21  \uD83D\uDE21 Supplier and Customer are NOT on the same node ..." +
                "  \uD83D\uDE21 flowSession(s) required")
        var supplierSession: FlowSession
        var customerSession: FlowSession
        var signedTransaction: SignedTransaction? = null
        Companion.logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 Invoice Registration: signInitialTransaction executed ...")
        if (supplierStatus == LOCAL_SUPPLIER && customerStatus == REMOTE_CUSTOMER) {
            Companion.logger.info(" \uD83D\uDE21  \uD83D\uDE21  \uD83D\uDE21 LOCAL_SUPPLIER and REMOTE_CUSTOMER ...")
            customerSession = initiateFlow(customerParty)
            signedTransaction = getSignedTransaction(signedTx, ImmutableList.of(customerSession))
        }
        if (supplierStatus == REMOTE_SUPPLIER && customerStatus == LOCAL_CUSTOMER) {
            Companion.logger.info(" \uD83D\uDE21  \uD83D\uDE21  \uD83D\uDE21 REMOTE_SUPPLIER and LOCAL_CUSTOMER ...")
            supplierSession = initiateFlow(supplierParty)
            signedTransaction = getSignedTransaction(signedTx, ImmutableList.of(supplierSession))
        }
        if (supplierStatus == REMOTE_SUPPLIER && customerStatus == REMOTE_CUSTOMER) {
            Companion.logger.info(" \uD83D\uDE21  \uD83D\uDE21  \uD83D\uDE21 REMOTE_SUPPLIER and REMOTE_CUSTOMER ...")
            supplierSession = initiateFlow(supplierParty)
            customerSession = initiateFlow(customerParty)
            signedTransaction = getSignedTransaction(signedTx, ImmutableList.of(supplierSession, customerSession))
        }
        if (signedTransaction != null) {
            reportToRegulator(serviceHub, signedTransaction)
        }
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
                " \uD83D\uDC4C \uD83D\uDC4C \uD83D\uDC4C OTHER NODE(S): FinalityFlow has been executed ... " +
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
