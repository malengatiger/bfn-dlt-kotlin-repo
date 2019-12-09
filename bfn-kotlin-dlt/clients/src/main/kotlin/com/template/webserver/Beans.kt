package com.template.webserver

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.context.support.beans
import org.springframework.core.env.Environment
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import java.io.FileInputStream

private val logger = LoggerFactory.getLogger(BeansInitializer::class.java)



fun beans() = beans {
    println("\uD83E\uDDE9  \uD83E\uDDE9  \uD83E\uDDE9  \uD83E\uDDE9 // Define your bean with Kotlin DSL here   \uD83E\uDDE9")
    bean {
        CommandLineRunner {
            logger.info("\uD83D\uDE21  CommandLineRunner \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 shit is gonna happen here")

        }
    }
    profile("cors") {
        bean { CorsWebFilter { CorsConfiguration().applyPermitDefaultValues() } }
        println("\uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06 " +
                "\uD83E\uDDE9  \uD83E\uDDE9  \uD83E\uDDE9  \uD83E\uDDE9 // CORS has been defined  \uD83E\uDDE9")
    }

    //506469654 malengatiger@gmail.com
}
