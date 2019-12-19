package com.template.webserver

import com.bfn.flows.invoices.BestOfferForInvoiceFlow
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.text.SimpleDateFormat
import java.util.*


@Component
class ScheduledTasks {
    private val logger = LoggerFactory.getLogger(ScheduledTasks::class.java)

    private val dateFormat = SimpleDateFormat("HH:mm:ss")
    @Autowired
    lateinit var context: ApplicationContext
    @Scheduled(fixedRate = 1000 * 60 * 3)
    fun startFindingBestOffers() {
        logger.info("\uD83C\uDF4F \uD83C\uDF4F startFindingBestOffers: " +
                "\uD83C\uDF4F  The time is now \uD83C\uDF4F  ${dateFormat.format(Date())} \uD83E\uDDE9")
        logger.info("\uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C Starting Auction (Finding Best Offers):  \uD83D\uDC7A ️${Date()} \uD83E\uDD6C")
        try {
            val admin = context.getBean(AdminController::class.java)
            val list = admin.runAuction()
            logger.info("\uD83C\uDF77 \uD83C\uDF77 \uD83C\uDF77 admin.runAuction found: \uD83E\uDDE9 ${list.size} tokens \uD83E\uDDE9 using context admin")
        } finally {
            //fixedRateTimer.cancel();
        }
    }
}
