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


    @Scheduled(cron = "0 0 * * * *")
    fun startFindingBestOffers() {
        runBestOffers()
    }
    @Scheduled(cron = "0 15 * * * *")
    fun startFindingBestOffersEvery15() {
        runBestOffers()
    }
    @Scheduled(cron = "0 30 * * * *")
    fun startFindingBestOffersEvery3minutes() {
        runBestOffers()
    }
    @Scheduled(cron = "0 45 * * * *")
    fun startFindingBestOffersEvery45minutes() {
        runBestOffers()
    }

    private fun runBestOffers() {
        logger.info("\uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 runBestOffers: \uD83C\uDF6F ")
        logger.info("\uD83C\uDF4F \uD83C\uDF4F startFindingBestOffers: " +
                "\uD83C\uDF4F  The time is now \uD83C\uDF4F  ${dateFormat.format(Date())} \uD83E\uDDE9")
        logger.info("\uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C Starting Auction (Finding Best Offers):  \uD83D\uDC7A ️${Date()} \uD83E\uDD6C")
        try {
            val admin = context.getBean(AdminController::class.java)
            val list = admin.runAuction()
            logger.info("\uD83C\uDF77 \uD83C\uDF77 \uD83C\uDF77 admin.runAuction found: \uD83E\uDDE9 ${list.size} tokens \uD83E\uDDE9 using context admin")
        } catch (e: Exception) {
            logger.error("Auto Auction failed", e)
        }
    }
}
