package com.bfn.client.web

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType.SERVLET
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.scheduling.annotation.EnableScheduling
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.full.functions

/**
 * Our Spring Boot application.
 */

fun main(args: Array<String>) {
    val app = SpringApplication(RestApiApplication::class.java)
    app.setBannerMode(Banner.Mode.OFF)
    app.webApplicationType = SERVLET
    app.run(*args)

    println("\uD83E\uDDE9 \uD83E\uDDE9 \uD83E\uDDE9 \uD83E\uDDE9 " +
            "BFN Web API (Kotlin) started \uD83E\uDDE9 \uD83E\uDDE9 \uD83E\uDDE9 \uD83E\uDDE9 ")

}

@SpringBootApplication
@EnableScheduling
private open class RestApiApplication: ApplicationListener<ApplicationReadyEvent> {
    private val logger = LoggerFactory.getLogger(RestApiApplication::class.java)

    @Autowired
    lateinit var context: ApplicationContext
    @Autowired
    private lateinit var appProperties: AppProperties

    override fun onApplicationEvent(contextRefreshedEvent: ApplicationReadyEvent) {
        logger.info("\uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C  STARTED SPRINGBOOT APP:  " +
                "\uD83E\uDDE9 onApplicationEvent: mainApplicationClass: " +
                "\uD83D\uDC7D ${contextRefreshedEvent.springApplication.mainApplicationClass}  \uD83D\uDC7D ")

        printAppInfo()
        setTimer()

    }

    private fun printAppInfo() {
        var cnt = 0
        val c = AdminController::class
        val functions = c.functions
        val sorted = functions.sortedBy { it.name }
        logger.info("\n..... \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C Functions available from AdminController")
        sorted.forEach() {
            cnt++
            logger.info("\uD83E\uDD6C AdminController Function: #$cnt \t\uD83C\uDF38 ${it.name} \uD83C\uDF38 ")
        }
        cnt = 0
        logger.info("\n..... \uD83E\uDDA0 \uD83E\uDDA0 \uD83E\uDDA0 \uD83E\uDDA0  Functions available from WorkerBee ...")
        val w = WorkerBee::class
        val collection = w.functions
        val sorted2 = collection.sortedBy { it.name }
        sorted2.forEach() {
            cnt++
            logger.info("\uD83C\uDF4E WorkerBee Function: #$cnt \t\uD83E\uDDA0  ${it.name} \uD83E\uDDA0 ")
        }
        logger.info("Pinging self, \uD83C\uDF56 \uD83C\uDF56 just for the hell of it!")
        val bean = context.getBean(AdminController::class.java)
        bean.ping()
        val flows = bean.getFlows()
        cnt = 0
        flows.forEach() {
            if (it.contains("com.bfn")) {
                cnt++
                logger.info("\uD83D\uDD37 Registered Corda Flow #$cnt : \uD83D\uDD37  $it  \uD83C\uDF4F")
            }
        }
    }

    private val dateFormat = SimpleDateFormat("HH:mm:ss")
    fun setTimer() {
        val bean = context.getBean(AdminController::class.java)
        val org: String = bean.getProxy().nodeInfo().legalIdentities.first().name.organisation
        logger.info("\n\uD83D\uDD35 \uD83D\uDD35 \uD83D\uDD35 \uD83D\uDD35 NODE \uD83C\uDF4E $org \uD83C\uDF4E " +
                "will start a Timer to control selectBestOffers for suppliers ...  ⏰  ⏰  ⏰ ")
        when (org) {
            "PartyA" -> startTimer( org,appProperties.intervalPartyA.toLong(), bean)
            "PartyB" -> startTimer( org, appProperties.intervalPartyB.toLong(), bean)
            "PartyC" -> startTimer( org, appProperties.intervalPartyC.toLong(), bean)
        }

    }
    fun startTimer(name: String, minutes: Long, bean: AdminController) {
        logger.info("\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E  startTimer:  \uD83C\uDF50 NODE: $name ️ ⏳ Interval in Minutes: $minutes  ⏰  ⏰  ⏰ ")
        val ms: Long = minutes * 1000 * 60
        Timer().schedule(object : TimerTask() {
            override fun run() {
                logger.info("\uD83C\uDF4E \uD83C\uDF4E \uD83C\uDF4E Timer TRIGGERED : The time is now \uD83C\uDF4F  ${dateFormat.format(Date())}" +
                        "\uD83C\uDF36 \uD83C\uDF36 \uD83C\uDF36  selectBestOffers on $name every $minutes minutes \uD83C\uDF4F  ")
                bean.selectBestOffers()
            }
        }, ms, ms)
    }


}