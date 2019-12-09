package com.template.webserver

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType.SERVLET
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.bind.annotation.RestController
import java.io.FileInputStream

/**
 * Our Spring Boot application.
 */
@SpringBootApplication
private open class Starter

private val logger = LoggerFactory.getLogger(RestController::class.java)

/**
 * Starts our Spring Boot application.
 */
fun main(args: Array<String>) {
    val app = SpringApplication(Starter::class.java)
    app.setBannerMode(Banner.Mode.OFF)
    app.webApplicationType = SERVLET
    app.run(*args)

    logger.info("\uD83E\uDDE9 \uD83E\uDDE9 \uD83E\uDDE9 \uD83E\uDDE9 " +
            "BFN Web API (Kotlin) started \uD83E\uDDE9 \uD83E\uDDE9 \uD83E\uDDE9 \uD83E\uDDE9 ")
    @Bean
    @Throws(Exception::class)
    fun firebaseBean(): FirebaseApp? {
        logger.info("\uD83D\uDD06  \uD83D\uDD06  \uD83D\uDD06  BFNWebApi:  setting up Firebase service account ...."
                + " \uD83D\uDD06  \uD83D\uDD06  \uD83D\uDD06")
        try {
            logger.info(("\uD83D\uDD06  \uD83D\uDD06  \uD83D\uDD06 ."
                    + "env PATH for Firebase Service Account: \uD83D\uDC99  ")
                    + firebasePath)
            val serviceAccount = FileInputStream(firebasePath)
            val options = FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://bfn-mobile-backend.firebaseio.com").build()
            val app = FirebaseApp.initializeApp(options)
            logger.info(" \uD83E\uDDE9\uD83E\uDDE9\uD83E\uDDE9  \uD83E\uDDE9\uD83E\uDDE9\uD83E\uDDE9 "
                    + "Firebase Admin SDK Setup OK:  \uD83E\uDDE9\uD83E\uDDE9\uD83E\uDDE9 name: "
                    + app.name)
        } catch (e: Exception) {
            logger.error(" \uD83D\uDC7F  \uD83D\uDC7F  \uD83D\uDC7F  \uD83D\uDC7F Firebase Admin SDK setup failed")
            throw Exception(" \uD83D\uDC7F  \uD83D\uDC7F unable to set Firebase up", e)
        }
        return null
    }
}
@Autowired @Value("\${firebasePath}")
val firebasePath: String? = null


