package com.template.dto

import java.util.*

class InvoiceDTO {
    var invoiceId: String? = null
    var invoiceNumber: String? = null
    var description: String? = null
    var amount = 0.0
    var totalAmount = 0.0
    var valueAddedTax = 0.0
    var dateRegistered: Date? = null
    var supplier: AccountInfoDTO? = null
    var customer: AccountInfoDTO? = null
    var supplierPublicKey: String? = null
    var customerPublicKey: String? = null

    constructor() {}
    constructor(invoiceId: String?, invoiceNumber: String?,
                description: String?, amount: Double, totalAmount: Double,
                valueAddedTax: Double, dateRegistered: Date?,
                supplier: AccountInfoDTO?, customer: AccountInfoDTO?, supplierPublicKey: String?, customerPublicKey: String?) {
        this.invoiceId = invoiceId
        this.invoiceNumber = invoiceNumber
        this.description = description
        this.amount = amount
        this.totalAmount = totalAmount
        this.valueAddedTax = valueAddedTax
        this.dateRegistered = dateRegistered
        this.supplier = supplier
        this.customer = customer
        this.supplierPublicKey = supplierPublicKey
        this.customerPublicKey = customerPublicKey
    }

}
