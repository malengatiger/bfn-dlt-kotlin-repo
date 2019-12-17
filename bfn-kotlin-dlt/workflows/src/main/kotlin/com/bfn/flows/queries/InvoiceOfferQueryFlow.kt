package com.bfn.flows.queries

import com.bfn.flows.services.InvoiceOfferFinderService
import com.template.states.InvoiceOfferState
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC

@StartableByRPC
class InvoiceOfferQueryFlow(
        private val investorId: String?,
        private val supplierId: String?) : FlowLogic<List<InvoiceOfferState>>() {

    @Throws(FlowException::class)
    override fun call(): List<InvoiceOfferState> {
        val serviceHub = serviceHub
        val service = serviceHub.cordaService(InvoiceOfferFinderService::class.java)
        if (investorId != null) {
            return service.getOffersForInvestor(investorId)
        }
        if (supplierId != null) {
            return service.getOffersForSupplier(supplierId)
        }
        return service.getOffersOnNode()
    }



}
