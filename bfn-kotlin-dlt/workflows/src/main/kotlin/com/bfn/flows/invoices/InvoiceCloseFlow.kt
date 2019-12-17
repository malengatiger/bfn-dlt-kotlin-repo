package com.bfn.flows.invoices

import co.paralleluniverse.fibers.Suspendable
import com.bfn.flows.regulator.BroadcastTransactionFlow
import com.bfn.flows.regulator.ReportToRegulatorFlow
import com.google.common.collect.ImmutableList
import com.r3.corda.lib.accounts.workflows.ourIdentity
import com.template.contracts.InvoiceContract
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
import java.security.PublicKey

@InitiatingFlow
@StartableByRPC
class InvoiceCloseFlow(private val invoiceId: String) : FlowLogic<SignedTransaction>() {

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
        val serviceHub = serviceHub
        Companion.logger.info(" \uD83E\uDD1F \uD83E\uDD1F  \uD83E\uDD1F \uD83E\uDD1F  " +
                "... InvoiceCloseFlow call started ...")
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        val page = serviceHub.vaultService.queryBy(
                criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED),
                contractStateType = InvoiceState::class.java,
                paging = PageSpecification(
                        pageNumber = 1, pageSize = 1000
                )
        )
        var invoiceState: StateAndRef<InvoiceState>? = null
        page.states.forEach() {
            if (it.state.data.invoiceId.toString() == invoiceId) {
                invoiceState = it
            }
        }
        if (invoiceState == null) {
            throw IllegalArgumentException("\uD83D\uDD8D Invoice not found")
        }
        Companion.logger.info("\uD83D\uDD8D \uD83D\uDD8D \uD83D\uDD8D " +
                "Close invoice: ${invoiceState!!.state.data.supplierInfo.name}  \uD83D\uDECE ${invoiceState!!.state.data.totalAmount}")
        val command = InvoiceContract.Close()
        val customerParty = invoiceState!!.state.data.customerInfo.host
        val supplierParty = invoiceState!!.state.data.supplierInfo.host
        val keys: MutableList<PublicKey> = mutableListOf()
        val map: MutableMap<String, Party> = mutableMapOf()
        map[customerParty.toString()] = customerParty
        map[supplierParty.toString()] = supplierParty
        map[serviceHub.ourIdentity.toString()] = serviceHub.ourIdentity
        map.forEach() {
            keys.add(it.value.owningKey)
        }
        val parties = map.values.toList()
        val txBuilder = TransactionBuilder(notary)
        txBuilder.addInputState(invoiceState!!)
        txBuilder.addCommand(command, keys)

        Companion.logger.info("\uD83D\uDD8D \uD83D\uDD8D \uD83D\uDD8D verify Transaction ... ")
        txBuilder.verify(serviceHub)

        val signedTx = serviceHub.signInitialTransaction(txBuilder)
        //Companion.logger.info("\uD83D\uDD8D \uD83D\uDD8D \uD83D\uDD8D finalizeTransaction ... ")
        //val mTx = finalizeTransaction(parties, signedTx)
        subFlow(BroadcastTransactionFlow(signedTx))

        Companion.logger.info("\uD83E\uDD16 \uD83E\uDD16 \uD83E\uDD16 \uD83E\uDD16 \uD83E\uDD16  " +
                "${invoiceState!!.state.data.supplierInfo.name} totalAmount: ${invoiceState!!.state.data.totalAmount}  " +
                "\uD83E\uDD16 CONSUMED !! \uD83E\uDD16 ")
        return signedTx;
    }

    @Suspendable
    private fun finalizeTransaction(
            parties: List<Party>,
            signedTx: SignedTransaction): SignedTransaction {
        val flowSessions: MutableList<FlowSession> = mutableListOf()
        parties.forEach() {
            if (it.toString() != serviceHub.ourIdentity.toString()) {
                flowSessions.add(initiateFlow(it))
            }
        }
        if (flowSessions.isEmpty()) {
            Companion.logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 " +
                    "All participants are LOCAL ... \uD83D\uDD06")
            val mSignedTransactionDone = subFlow(
                    FinalityFlow(signedTx, ImmutableList.of<FlowSession>()))
            Companion.logger.info("\uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D  SAME NODE ==> " +
                    " \uD83E\uDD66 \uD83E\uDD66  \uD83E\uDD66 \uD83E\uDD66 FinalityFlow has been executed " +
                    "...\uD83E\uDD66 \uD83E\uDD66")
            return mSignedTransactionDone
        } else {
            Companion.logger.info(" \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 " +
                    "Participants are LOCAL/REMOTE ... \uD83D\uDD06")
            val tx = collectSignatures(signedTx, flowSessions)
            val mSignedTransactionDone = subFlow(
                    FinalityFlow(tx,flowSessions))
            Companion.logger.info("\uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D  ${flowSessions.size} remote NODES involved ==> " +
                    " \uD83E\uDD66 \uD83E\uDD66  \uD83E\uDD66 \uD83E\uDD66 FinalityFlow has been executed " +
                    "...\uD83E\uDD66 \uD83E\uDD66")
            return mSignedTransactionDone
        }
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
        private val logger = LoggerFactory.getLogger(InvoiceCloseFlow::class.java)

    }


}
