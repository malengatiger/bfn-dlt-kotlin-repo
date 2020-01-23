package cloudcode.bfn.web;

import cloudcode.bfn.BFNWebApplication;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.annotation.PostConstruct;
@Component
public class FirebaseScaffold {

    private final static Logger logger = LoggerFactory.getLogger(FirebaseScaffold.class.getSimpleName());
    private Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    @Autowired
    private ApplicationContext context;
    @PostConstruct
    public void init() throws IOException {
        logger.info("\n\n\uD83D\uDC13 \uD83D\uDC13 \uD83D\uDC13 FirebaseScaffold  \uD83C\uDF51 " +
                "init: starting to fuck with \uD83D\uDC13  \uD83C\uDF51 Firebase  \uD83C\uDF51 \uD83D\uDC13 \uD83D\uDC13 \n");
        logger.info("\uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D \uD83D\uDC7D " +
                "PostConstruct: \uD83C\uDF3F Alexa and AI are coming for you!  \uD83C\uDF4F "
                        .contains(context.getApplicationName().concat("  \uD83C\uDF4F ")));

        logger.info("\uD83D\uDC4C\uD83C\uDFFE \uD83D\uDC4C\uD83C\uDFFE YEBO! appProperties are cool. " +
                "\uD83C\uDF4F \uD83C\uDF4F \uD83C\uDF4F this shit is hanging in there ..." );

        logger.info("\uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06  FirebaseScaffold: setting up Firebase service account ...."
                + " \uD83D\uDD06 \uD83D\uDD06 \uD83D\uDD06");
        try {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .setProjectId("bfn-mobile-backend")
                    .setDatabaseUrl("https://bfn-mobile-backend.firebaseio.com").build();
            FirebaseApp app = FirebaseApp.initializeApp(options);
            logger.info("\uD83E\uDDE9\uD83E\uDDE9\uD83E\uDDE9  \uD83E\uDDE9\uD83E\uDDE9\uD83E\uDDE9 "
                    + "Firebase Admin SDK Setup OK:  \uD83E\uDDE9\uD83E\uDDE9\uD83E\uDDE9 app: "
                    + app.toString());
            //
            //listAccountsFromFirebase();
            logger.info("\uD83D\uDD06  \uD83D\uDD06  \uD83D\uDD06  Getting bean to start refreshing nodes on Firestore  \uD83D\uDD06 ");
            try {
                BFNController adminController = context.getBean(BFNController.class);
                logger.info(("\uD83C\uDF4E Forcing BFN to say Hello!  \uD83D\uDC38 \uD83D\uDC38 \uD83D\uDD90\uD83C\uDFFD " +
                        "\uD83D\uDD90\uD83C\uDFFD \uD83D\uDD90\uD83C\uDFFD ")
                        .concat(adminController.helloWorld()));
            } catch (Exception e) {
                logger.error(e.getMessage());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
