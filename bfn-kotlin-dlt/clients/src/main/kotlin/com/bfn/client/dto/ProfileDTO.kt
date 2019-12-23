package com.bfn.client.dto

import net.corda.core.identity.Party
import java.math.BigDecimal
import java.util.*


data class ProfileDTO(var issuedBy: Party,
                   val accountId: String,
                   val minimumDiscount: Double,
                   val minimumInvoiceAmount: BigDecimal,
                   val maximumInvoiceAmount: BigDecimal,
                   val maximumInvestmentPerInvoice: BigDecimal,
                   val maximumTotalInvestment: BigDecimal,
                   var date: Date
)
