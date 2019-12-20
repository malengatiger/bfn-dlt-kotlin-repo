package com.template.dto

import net.corda.core.identity.Party
import java.util.*

data class ProfileStateDTO (val issuedBy: String?,
                            val accountId: String,
                            val minimumDiscount: Double,
                            val minimumInvoiceAmount: Double,
                            val maximumInvoiceAmount: Double,
                            val maximumInvestmentPerInvoice: Double,
                            val maximumTotalInvestment: Double,
                            val defaultDiscount: Double,
                            var date: Date)
