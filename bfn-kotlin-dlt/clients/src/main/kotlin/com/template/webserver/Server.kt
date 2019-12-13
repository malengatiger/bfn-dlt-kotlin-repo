package com.template.webserver

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType.SERVLET
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import javax.annotation.PostConstruct

/**
 * Our Spring Boot application.
 */
//@SpringBootApplication
//@ConfigurationProperties()
//private open class AppStarter
//
//private val logger = LoggerFactory.getLogger(AppStarter::class.java)
//
///**
// * Starts our Spring Boot application.
// */
fun main(args: Array<String>) {
    val app = SpringApplication(RestApiApplication::class.java)
    app.setBannerMode(Banner.Mode.OFF)
    app.webApplicationType = SERVLET
    app.run(*args)

    println("\uD83E\uDDE9 \uD83E\uDDE9 \uD83E\uDDE9 \uD83E\uDDE9 " +
            "BFN Web API (Kotlin) started \uD83E\uDDE9 \uD83E\uDDE9 \uD83E\uDDE9 \uD83E\uDDE9 ")

}

@SpringBootApplication
private open class RestApiApplication: ApplicationListener<ApplicationReadyEvent> {
    private val logger = LoggerFactory.getLogger(RestApiApplication::class.java)

    override fun onApplicationEvent(contextRefreshedEvent: ApplicationReadyEvent) {
        logger.info("\uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C  STARTED SPRING BOOT APP:  " +
                "\uD83E\uDDE9 onApplicationEvent: ")
    }

}
