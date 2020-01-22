package com.bfnconfig;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.logging.Logger;

@EnableConfigServer
@SpringBootApplication
public class BfnconfigApplication implements CommandLineRunner, ApplicationListener {

	static  final Logger LOGGER = Logger.getLogger(BfnconfigApplication.class.getName());

//	public BfnconfigApplication(SpringApplication application, String[] args, ConfigurableApplicationContext context) {
//		super(application, args, context);
//		LOGGER.info("\uD83E\uDD80 \uD83E\uDD80 BfnconfigApplication \uD83E\uDD80 starting .... :: \uD83E\uDD80 ".concat(Objects.requireNonNull(context.getId())));
//		Iterator<String> m = context.getBeanFactory().getBeanNamesIterator();
//		while (m.hasNext()) {
//			LOGGER.info("\uD83E\uDD80 Bean: " + m.next() + " \uD83E\uDD80 ");
//		}
//
//	}

	public static void main(String[] args) {
		LOGGER.info("\uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C BfnconfigApplication starting ....");
		SpringApplication.run(BfnconfigApplication.class, args);
		LOGGER.info("BfnconfigApplication started .... \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C :: ".concat(new Date().toString()));

	}
	@Autowired
	private ApplicationContext appContext;
	@Override
	public void run(String... args) throws Exception {
		LOGGER.info("\n☘️ ☘️ ☘️ CommandLineRunner override code here! ☘️ ☘️ ☘️ \uD83E\uDD80 \n");

	}

	@Override
	public void onApplicationEvent(ApplicationEvent applicationEvent) {
		if (applicationEvent instanceof ApplicationStartedEvent) {
			LOGGER.info("\n\uD83C\uDD7F️\uD83C\uDD7F️\uD83C\uDD7F️\uD83C\uDD7F️ CONFIG SERVER IS NOW OPENING THE DOORS : \uD83C\uDF3A Just a few seconds now ... \uD83C\uDF3A ");
		}
		String name = applicationEvent.getClass().getName();
		int index = name.lastIndexOf(".");
		if (index > - 1) {
			String m = name.substring(index + 1);
			LOGGER.info("\uD83C\uDD7F️ \uD83C\uDD7F️ \uD83C\uDD7F️ \uD83C\uDD7F️ onApplicationEvent: \uD83C\uDF3A " + m);

		} else {
			LOGGER.info("\uD83C\uDD7F️ onApplicationEvent: \uD83C\uDF3A " + name);
		}
		String[] m = appContext.getEnvironment().getActiveProfiles();
		if (m.length > 0) {
			LOGGER.info(m[0]);
		} else {
			LOGGER.info("\uD83E\uDD80 No active profiles. maybe that's cool, no? \uD83E\uDD80 \uD83E\uDD80 ");
		}
		LOGGER.info("\uD83D\uDECE \uD83D\uDECE DisplayName: \uD83D\uDECE  " + appContext.getDisplayName() + "  \uD83C\uDD7F️ Id: " +  appContext.getId());
		if (applicationEvent instanceof ApplicationReadyEvent) {
			LOGGER.info("\n\uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C CONFIG SERVER IS NOW OPEN FOR BUSINESS : \uD83C\uDF3A We serve application properties \uD83C\uDF3A ");
		}

	}
}
