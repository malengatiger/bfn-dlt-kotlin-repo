package cloudcode.bfn.web;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class FirebaseUtil {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();
    private static final Firestore db = FirestoreClient.getFirestore();
    private static final FirebaseMessaging messaging = FirebaseMessaging.getInstance();
    private final static Logger logger = LoggerFactory.getLogger(FirebaseUtil.class.getSimpleName());

    public static void getAccounts() {
        try {
            int cnt = 0;
            for (QueryDocumentSnapshot accounts : db.collection("accounts").get().get().getDocuments()) {
                cnt++;
                logger.info("\uD83D\uDE21 \uD83D\uDE21 Account: \uD83D\uDE21 ".concat(" #" + cnt + " \uD83D\uDCA6 ")
                        .concat(accounts.getData().get("name").toString()
                                .concat(" \uD83C\uDF4B \uD83C\uDF4B ")));
            }
            logger.info("\uD83C\uDF4E \uD83C\uDF4E There are \uD83C\uDF4E \uD83C\uDF4E " + cnt + "  \uD83C\uDF4E \uD83C\uDF4E " +
                    "accounts on the Node (\uD83E\uDD6C \uD83E\uDD6C in Firestore \uD83E\uDD6C \uD83E\uDD6C)");
        } catch (InterruptedException | ExecutionException e) {
            logger.warn(e.getMessage());
        }
    }
}
