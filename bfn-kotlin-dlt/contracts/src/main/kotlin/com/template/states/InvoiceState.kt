package com.template.states

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.template.InvoiceContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable
import org.slf4j.LoggerFactory
import java.util.*

// *********
// * State *
// *********
@BelongsToContract(InvoiceContract::class)
@CordaSerializable
class InvoiceState(val invoiceId: UUID,
                   val invoiceNumber: String,
                   val description: String,
                   val amount: Amount<TokenType>,
                   val supplierInfo: AccountInfo,
                   val customerInfo: AccountInfo,
                   val dateRegistered: Date?) : ContractState {
    override val participants: List<AbstractParty>
        get() = Arrays.asList(supplierInfo.host,
                customerInfo.host)

    companion object {
        private val logger = LoggerFactory.getLogger(InvoiceState::class.java)
    }


}
