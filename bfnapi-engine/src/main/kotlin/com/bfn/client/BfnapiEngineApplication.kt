package com.bfn.client

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BfnapiEngineApplication  : SpringBootServletInitializer()

fun main(args: Array<String>) {

	runApplication<BfnapiEngineApplication>(*args)
}
