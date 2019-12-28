package com.bfn.client.web

import com.google.gson.GsonBuilder
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.loggerFor

fun main(args: Array<String>) = InvoiceIntegration().main(args)
class InvoiceIntegration {
    companion object {
        val logger = loggerFor<InvoiceIntegration>()
        private val GSON = GsonBuilder().setPrettyPrinting().create()

    }
    fun main(args: Array<String>) {

    }

    fun integrateInvoices(proxy: CordaRPCOps) {
        //todo - get invoices from accessing CUSTOMER database/file etc. and associate them with accounts on the node
    }
}
