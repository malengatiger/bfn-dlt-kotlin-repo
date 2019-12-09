package com.template.states

import com.google.common.collect.ImmutableList
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.template.InvoiceContract
import com.template.InvoiceOfferContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.*

@CordaSerializable
@BelongsToContract(InvoiceOfferContract::class)
class InvoiceOfferState(val invoiceId: UUID, val offerAmount: BigDecimal, val discount: BigDecimal,
                        val originalAmount: BigDecimal, val supplier: AccountInfo,
                        val investor: AccountInfo, val owner: AccountInfo, val offerDate: Date,
        //
                        val ownerDate: Date, val invoiceNumber: String, val customer: AccountInfo) : ContractState {
    override val participants: List<AbstractParty>
        get() = ImmutableList.of<AbstractParty>(supplier.host,
                investor.host, customer.host)

    //    @NotNull
//    @Override
//    public PersistentState generateMappedObject(@NotNull MappedSchema schema) {
//        if (schema instanceof InvoiceOfferSchemaV1) {
//            logger.info("\uD83D\uDCA6 \uD83D\uDCA6 \uD83D\uDCA6 PersistentState generateMappedObject returning new object: \uD83D\uDCA6 PersistentInvoiceOffer");
//            return new InvoiceOfferSchemaV1.PersistentInvoiceOffer(
//                    this.invoiceId,this.offerAmount,
//                    this.discount, this.supplier,this.investor, this.owner,
//                    this.offerDate,this.ownerDate,this.supplierPublicKey,
//                    this.investorPublicKey);
//        } else {
//            throw new IllegalArgumentException("Object fucked");
//        }
//    }
//
//    @NotNull
//    @Override
//    public Iterable<MappedSchema> supportedSchemas() {
//        return ImmutableList.of(new InvoiceOfferSchemaV1());
//    }
    companion object {
        private val logger = LoggerFactory.getLogger(InvoiceContract::class.java)
    }

}
