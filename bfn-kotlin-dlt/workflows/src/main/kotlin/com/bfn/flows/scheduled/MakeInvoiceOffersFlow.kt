package com.bfn.flows.scheduled

import co.paralleluniverse.fibers.Suspendable
import com.bfn.flows.services.InvoiceFinderService
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
import org.slf4j.LoggerFactory
import java.security.PublicKey
import java.util.*

@InitiatingFlow
@StartableByRPC
class MakeInvoiceOffersFlow(private val investorId: String) : FlowLogic<List<InvoiceOfferState>?>() {

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): List<InvoiceOfferState> {
        Companion.logger.info("\uD83E\uDD63 \uD83E\uDD63 \uD83E\uDD63 \uD83E\uDD63 " +
                "MakeInvoiceOfferFlow: \uD83C\uDF45 Select INVOICES for investor and make OFFERS ... \uD83E\uDD63 ")
        val invoiceService = serviceHub.cordaService(InvoiceFinderService::class.java)
        val invoices = invoiceService.findInvoicesForInvestor(investorId)
        if (invoices.isEmpty()) {
            return listOf()
        }
        val account = serviceHub.accountService.accountInfo(UUID.fromString(investorId))
                ?: throw IllegalArgumentException("Account not found")
        Companion.logger.info("\uD83E\uDD63 \uD83E\uDD63 ${invoices.size} Total invoices selected for investor: " +
                "\uD83E\uDD66 ${account!!.state.data.name} \uD83E\uDD66")
        val profile = invoiceService.findProfile(investorId)
                ?: throw IllegalArgumentException("\uD83D\uDC7F Investor Profile not found")
        //make offers
        val partiesMap: MutableMap<String, Party> = mutableMapOf()
        val offers = createOffers(partiesMap, invoices, account, profile)
        val command = InvoiceOfferContract.MakeOffer()
        val parties = partiesMap.values.toList()
        val keys: MutableList<PublicKey> = mutableListOf()
        parties.forEach() {
            keys.add(it.owningKey)
        }
        processTransaction(command, keys, offers, parties, account)
        return offers
    }

    @Suspendable
    @Throws(FlowException::class)
    private fun processTransaction(command: InvoiceOfferContract.MakeOffer,
                                   keys: MutableList<PublicKey>,
                                   offers: List<InvoiceOfferState>,
                                   parties: List<Party>,
                                   account: StateAndRef<AccountInfo>) {
        val txBuilder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.first())
        txBuilder.addCommand(command, keys)
        offers.forEach() {
            txBuilder.addOutputState(it)
        }
        txBuilder.verify(serviceHub)
        val partlySigned = serviceHub.signInitialTransaction(txBuilder)

        Companion.logger.info("\uD83E\uDD63 \uD83E\uDD63 \uD83E\uDD63 MakeInvoiceOffersFlow " +
                "- transaction verified and signed initially, parties: ${parties.size}, offers: ${offers.size}")
        if (parties.size == 1) {
            Companion.logger.info("\uD83E\uDD63 \uD83E\uDD63 \uD83E\uDD63 MakeInvoiceOffersFlow " +
                    "- about to call FinalityFlow with no sessions; party is: ${parties[0]}")
            subFlow(FinalityFlow(partlySigned, listOf()))
        } else {
            Companion.logger.info("\uD83E\uDD63 \uD83E\uDD63 \uD83E\uDD63 MakeInvoiceOffersFlow " +
                    "- about to create flowSessions with ${parties.size} parties")
            val flowSessions: MutableList<FlowSession> = mutableListOf()
            parties.forEach() {
                if (it.toString() != serviceHub.ourIdentity.toString()) {
                    flowSessions.add(initiateFlow(it))
                }
            }
            Companion.logger.info("\uD83E\uDD63 \uD83E\uDD63 \uD83E\uDD63 MakeInvoiceOffersFlow " +
                    "- about to call collectSignaturesAndFinalize with ${flowSessions.size} sessions")
            collectSignaturesAndFinalize(partlySigned, flowSessions)
        }
        Companion.logger.info("\uD83E\uDD63 \uD83E\uDD63 \uD83E\uDD63 \uD83E\uDD63 " +
                "MakeInvoiceOfferFlow: " +
                " : ${offers.size} InvoiceOffers made for investor: " +
                "${account.state.data.name} \uD83D\uDECE\uD83D\uDECE ")
    }

    @Suspendable
    @Throws(FlowException::class)
    private fun createOffers(partiesMap: MutableMap<String, Party>,
                             invoices: List<InvoiceState>,
                             account: StateAndRef<AccountInfo>?,
                             profile: ProfileState?): List<InvoiceOfferState> {
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
                    offerAmount = getOfferAmount(it.totalAmount, profile.defaultDiscount),
                    offerDate = Date(),
                    originalAmount = it.totalAmount,
                    supplier = it.supplierInfo, ownerDate = Date()
            ))
        }
        return offers
    }

    @Suspendable
    @Throws(FlowException::class)
    private fun collectSignaturesAndFinalize(signedTx: SignedTransaction, sessions: List<FlowSession>): SignedTransaction? {

        val signedTransaction: SignedTransaction
        try {
            signedTransaction = subFlow(CollectSignaturesFlow(
                    partiallySignedTx = signedTx, sessionsToCollectFrom = sessions))
            Companion.logger.info("\uD83C\uDFBD \uD83C\uDFBD \uD83C\uDFBD \uD83C\uDFBD  " +
                    "${sessions.size} Signatures collected OK!  \uD83D\uDE21 \uD83D\uDE21 " )
        } catch (e: Exception) {
            logger.error("\uD83D\uDE21 \uD83D\uDC7F Signature Collection failed; ${sessions.size} sessions", e)
            logger.info("signedTx \uD83C\uDF54 \uD83C\uDF54 signatures:: ${signedTx.sigs.size}")
            sessions.forEach() {
                logger.info("\uD83C\uDF3C \uD83C\uDF3C session counterParty: ${it.counterparty}")
            }

            throw IllegalArgumentException("\uD83D\uDC79 Houston, \uD83D\uDC79 \uD83D\uDC7A we fucked!! \uD83D\uDC79 cannot collect signatures")
        }

        Companion.logger.info("\uD83C\uDFBD \uD83C\uDFBD \uD83C\uDFBD \uD83C\uDFBD  " +
                "collectSignaturesAndFinalize: .... will call FinalityFlow ... \uD83C\uDF3A \uD83C\uDF3A ")

        val mSignedTransactionDone = subFlow(
                FinalityFlow(signedTransaction, sessions))

        Companion.logger.info("\uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D  " +
                " \uD83D\uDC4C \uD83D\uDC4C \uD83D\uDC4C  \uD83E\uDD66 \uD83E\uDD66  " +
                "\uD83E\uDD66 \uD83E\uDD66  \uD83E\uDD66 \uD83E\uDD66 MULTIPLE NODE(S): FinalityFlow has been executed ... " +
                "\uD83E\uDD66 \uD83E\uDD66")

        return mSignedTransactionDone
    }

    @Suspendable
    @Throws(FlowException::class)
    private fun getOfferAmount(invoiceAmount: Double, discount: Double): Double {
        val percentage = 100.0 - discount
        val offerAmt = invoiceAmount * (percentage/100)
        Companion.logger.info("\uD83D\uDD30 \uD83D\uDD30 Offer amount is " +
                "$offerAmt calculated from \uD83D\uDD30 $invoiceAmount with discount: $discount")
        return offerAmt
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MakeInvoiceOffersFlow::class.java)

    }
}
