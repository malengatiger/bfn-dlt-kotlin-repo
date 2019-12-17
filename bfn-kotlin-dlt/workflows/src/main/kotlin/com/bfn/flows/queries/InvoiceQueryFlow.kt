package com.bfn.flows.queries

import com.bfn.flows.services.InvoiceFinderService
import com.template.states.InvoiceState
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC

@StartableByRPC
class InvoiceQueryFlow(
        private val investorId: String?,
        private val supplierId: String?) : FlowLogic<List<InvoiceState>>() {

    @Throws(FlowException::class)
    override fun call(): List<InvoiceState> {
        val serviceHub = serviceHub
        val service = serviceHub.cordaService(InvoiceFinderService::class.java)
        if (investorId != null) {
            return service.findInvoicesForInvestor(investorId)
        }
        if (supplierId != null) {
            return service.findInvoicesForSupplier(supplierId)
        }
        return service.findInvoicesForNode()
    }



}
