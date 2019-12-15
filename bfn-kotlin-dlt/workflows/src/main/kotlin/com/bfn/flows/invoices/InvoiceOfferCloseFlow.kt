package com.bfn.flows.invoices

import co.paralleluniverse.fibers.Suspendable
import com.bfn.flows.regulator.ReportToRegulatorFlow
import com.google.common.collect.ImmutableList
import com.template.contracts.InvoiceContract
import com.template.states.InvoiceOfferState
import com.template.states.InvoiceState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import org.slf4j.LoggerFactory

@InitiatingFlow
@StartableByRPC
class InvoiceOfferCloseFlow(private val stateAndRef: StateAndRef<InvoiceOfferState>) : FlowLogic<SignedTransaction>() {

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
        val serviceHub = serviceHub
        Companion.logger.info(" \uD83E\uDD1F \uD83E\uDD1F  \uD83E\uDD1F \uD83E\uDD1F  ... InvoiceOfferCloseFlow call started ...")
        val notary = serviceHub.networkMapCache.notaryIdentities[0]


        val command = InvoiceContract.Close()
        val investorParty = stateAndRef.state.data.investor.host
        val supplierParty = stateAndRef!!.state.data.supplier.host

        val txBuilder = TransactionBuilder(notary)
        txBuilder.addInputState(stateAndRef)
        txBuilder.addCommand(command, supplierParty.owningKey,
                investorParty.owningKey)

        txBuilder.verify(serviceHub)
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        val mTx = finalizeTransaction(serviceHub, investorParty, supplierParty, signedTx)
        Companion.logger.info("\uD83E\uDD16 \uD83E\uDD16 \uD83E\uDD16 \uD83E\uDD16 \uD83E\uDD16  " +
                "${stateAndRef.state.data.supplier.name} offerAmount: ${stateAndRef.state.data.offerAmount} investor: ${stateAndRef.state.data.investor.name} " +
                "\uD83E\uDD16 CONSUMED !! \uD83E\uDD16 ")
        return mTx;
    }

    @Suspendable
    private fun finalizeTransaction(serviceHub: ServiceHub,
                                    investorParty: Party,
                                    supplierParty: Party,
                                    signedTx: SignedTransaction): SignedTransaction {
        val nodeInfo = serviceHub.myInfo
        val investorOrg: String = investorParty.name.organisation
        val supplierOrg: String = supplierParty.name.organisation
        val thisNodeOrg = nodeInfo.legalIdentities.first().name.organisation

        val supplierStatus: Int
        val investorStatus: Int
        val supplierSession: FlowSession
        val investorSession: FlowSession
        var signedTransaction: SignedTransaction? = null
        supplierStatus = if (supplierOrg.equals(thisNodeOrg, ignoreCase = true)) {
            LOCAL_SUPPLIER
        } else {
            REMOTE_SUPPLIER
        }
        investorStatus = if (investorOrg.equals(thisNodeOrg, ignoreCase = true)) {
            LOCAL_investor
        } else {
            REMOTE_investor
        }
        if (supplierStatus == LOCAL_SUPPLIER && investorStatus == LOCAL_investor) {
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
                "Supplier and/or investor are NOT on the same node ..." +
                "  \uD83D\uDE21 flowSession(s) required \uD83D\uDE21")

        if (supplierStatus == LOCAL_SUPPLIER && investorStatus == REMOTE_investor) {
            Companion.logger.info(" \uD83D\uDE21  \uD83D\uDE21 \uD83D\uDE21 " +
                    "investor is REMOTE - Supplier is LOCAL \uD83D\uDE21 ")
            investorSession = initiateFlow(investorParty)
            signedTransaction = collectSignatures(signedTx, ImmutableList.of(
                    investorSession))
            return signedTransaction
        }
        if (supplierStatus == REMOTE_SUPPLIER && investorStatus == REMOTE_investor) {
            Companion.logger.info(" \uD83D\uDE21  \uD83D\uDE21 \uD83D\uDE21 " +
                    "Supplier and investor are REMOTE \uD83D\uDE21 ")
            investorSession = initiateFlow(investorParty)

            return if (investorOrg == supplierOrg) {
                signedTransaction = collectSignatures(signedTx, ImmutableList.of(
                        investorSession))
                signedTransaction
            } else {
                supplierSession = initiateFlow(supplierParty)
                signedTransaction = collectSignatures(signedTx, ImmutableList.of(
                        investorSession, supplierSession))
                signedTransaction
            }

        }
        if (supplierStatus == REMOTE_SUPPLIER && investorStatus == LOCAL_investor) {
            Companion.logger.info(" \uD83D\uDE21  \uD83D\uDE21 \uD83D\uDE21 " +
                    "Supplier is REMOTE - investor is LOCAL \uD83D\uDE21 ")
            supplierSession = initiateFlow(supplierParty)
            signedTransaction = collectSignatures(signedTx, ImmutableList.of(
                    supplierSession))
            return signedTransaction
        }

        return signedTransaction!!
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


    companion object {
        private val logger = LoggerFactory.getLogger(InvoiceOfferCloseFlow::class.java)
        private const val LOCAL_SUPPLIER = 1
        private const val LOCAL_investor = 2
        private const val REMOTE_SUPPLIER = 3
        private const val REMOTE_investor= 4
    }


}
