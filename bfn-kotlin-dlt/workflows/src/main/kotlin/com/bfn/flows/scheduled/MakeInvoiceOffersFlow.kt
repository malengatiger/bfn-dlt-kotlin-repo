package com.bfn.flows.scheduled

import co.paralleluniverse.fibers.Suspendable
import com.bfn.flows.services.InvoiceFinderService
import com.bfn.flows.services.InvoiceOfferFinderService
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.corda.lib.accounts.workflows.ourIdentity
import com.template.InvoiceOfferContract
import com.template.states.InvoiceOfferState
import com.template.states.InvoiceState
import com.template.states.ProfileState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.security.PublicKey
import java.util.*

@InitiatingFlow
@StartableByRPC
class MakeInvoiceOffersFlow(private val investorId: String) : FlowLogic<List<InvoiceOfferState>?>() {

    @Suspendable
    override fun call(): List<InvoiceOfferState> {
        logger.info("\uD83E\uDD63 \uD83E\uDD63 \uD83E\uDD63 \uD83E\uDD63 " +
                "MakeInvoiceOfferFlow: \uD83C\uDF45 Select INVOICES for investor and make OFFERS ... \uD83E\uDD63 ")
        val invoiceService = serviceHub.cordaService(InvoiceFinderService::class.java)
        val invoices = invoiceService.findInvoicesForInvestor(investorId)

        val account = serviceHub.accountService.accountInfo(UUID.fromString(investorId))
        logger.info("\uD83E\uDD63 \uD83E\uDD63 ${invoices.size} Total invoices selected for investor: " +
                "\uD83E\uDD66 ${account!!.state.data.name} \uD83E\uDD66")
        val profile = invoiceService.findProfile(investorId)
                ?: throw IllegalArgumentException("\uD83D\uDC7F Investor Profile not found")
        //make offers
        val partiesMap: MutableMap<String,Party> = mutableMapOf()
        val offers = createOffers(partiesMap, invoices, account, profile)
        val command = InvoiceOfferContract.MakeOffer()
        val parties = partiesMap.values.toList()
        val keys: MutableList<PublicKey> = mutableListOf()
        parties.forEach() {
            keys.add(it.owningKey)
        }
        val txBuilder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.first())
        txBuilder.addCommand(command, keys)
        offers.forEach() {
            txBuilder.addOutputState(it)
        }
        txBuilder.verify(serviceHub)
        val partlySigned = serviceHub.signInitialTransaction(txBuilder)
        if (parties.size == 1) {
            subFlow(FinalityFlow(partlySigned, listOf()))
        } else {
            val sessions: MutableList<FlowSession> = mutableListOf()
            parties.forEach() {
                if (it.toString() != serviceHub.ourIdentity.toString()) {
                    sessions.add(initiateFlow(it))
                }
            }
            collectSignaturesAndFinalize(partlySigned,sessions)
        }
        logger.info("\uD83E\uDD63 \uD83E\uDD63 \uD83E\uDD63 \uD83E\uDD63 " +
                "MakeInvoiceOfferFlow: " +
                " : ${offers.size} InvoiceOffers made for investor: " +
                "${account.state.data.name} \uD83D\uDECE\uD83D\uDECE ")
        return offers
    }

    private fun createOffers(partiesMap: MutableMap<String, Party>,
                             invoices: List<InvoiceState>,
                             account: StateAndRef<AccountInfo>?,
                             profile: ProfileState?) : List<InvoiceOfferState> {
        partiesMap[serviceHub.ourIdentity.toString()] = serviceHub.ourIdentity
        val offers: MutableList<InvoiceOfferState> = mutableListOf()
        invoices.forEach() {
            partiesMap[it.supplierInfo.host.toString()] = it.supplierInfo.host
            partiesMap[it.customerInfo.host.toString()] = it.customerInfo.host

            offers.add(InvoiceOfferState(
                    invoiceId = it.invoiceId,
                    customer = it.customerInfo,
                    investor = account!!.state.data,
                    discount = profile!!.defaultDiscount,
                    invoiceNumber = it.invoiceNumber,
                    offerAmount = getOfferAmount(it.totalAmount, profile!!.defaultDiscount),
                    offerDate = Date(),
                    originalAmount = it.totalAmount,
                    supplier = it.supplierInfo, ownerDate = Date()
            ))
        }
        return offers
    }

    @Suspendable
    @Throws(FlowException::class)
    private fun collectSignaturesAndFinalize(signedTx: SignedTransaction, sessions: List<FlowSession>): SignedTransaction {

        val signedTransaction = subFlow(CollectSignaturesFlow(
                partiallySignedTx = signedTx, sessionsToCollectFrom = sessions))
        logger.info("\uD83C\uDFBD \uD83C\uDFBD \uD83C\uDFBD \uD83C\uDFBD  " +
                "Signatures collected OK!  \uD83D\uDE21 \uD83D\uDE21 " +
                ".... will call FinalityFlow ... \uD83C\uDF3A \uD83C\uDF3A txId: "
                + signedTransaction.id.toString())

        val mSignedTransactionDone = subFlow(
                FinalityFlow(signedTransaction, sessions))

        logger.info("\uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D  " +
                " \uD83D\uDC4C \uD83D\uDC4C \uD83D\uDC4C  \uD83E\uDD66 \uD83E\uDD66  " +
                "\uD83E\uDD66 \uD83E\uDD66  \uD83E\uDD66 \uD83E\uDD66 MULTIPLE NODE(S): FinalityFlow has been executed ... " +
                "\uD83E\uDD66 \uD83E\uDD66")

        return mSignedTransactionDone
    }
    @Suspendable
    private fun getOfferAmount(invoiceAmount:Double, discount: Double): Double {
        val perc = 100.0 - discount / 100.0
        return invoiceAmount * perc
    }

}
