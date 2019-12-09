package com.template.dto

import java.util.*

class InvoiceOfferDTO {
    var invoiceId: String? = null
    var invoiceNumber: String? = null
    var offerAmount = 0.0
    var discount = 0.0
    var originalAmount = 0.0
    var supplier: AccountInfoDTO? = null
    var investor: AccountInfoDTO? = null
    var owner: AccountInfoDTO? = null
    var customer: AccountInfoDTO? = null
    var offerDate: Date? = null
    var investorDate: Date? = null
    var supplierPublicKey: String? = null
    var investorPublicKey: String? = null

    constructor(invoiceId: String?, invoiceNumber: String?,
                offerAmount: Double, discount: Double,
                originalAmount: Double,
                supplier: AccountInfoDTO?, investor: AccountInfoDTO?,
                owner: AccountInfoDTO?, offerDate: Date?, investorDate: Date?,
                supplierPublicKey: String?, investorPublicKey: String?, customer: AccountInfoDTO?) {
        this.invoiceId = invoiceId
        this.offerAmount = offerAmount
        this.discount = discount
        this.originalAmount = originalAmount
        this.supplier = supplier
        this.investor = investor
        this.owner = owner
        this.offerDate = offerDate
        this.investorDate = investorDate
        this.supplierPublicKey = supplierPublicKey
        this.investorPublicKey = investorPublicKey
        this.invoiceNumber = invoiceNumber
        this.customer = customer
    }

    constructor() {}

}
