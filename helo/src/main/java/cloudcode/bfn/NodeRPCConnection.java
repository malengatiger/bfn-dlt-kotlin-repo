package cloudcode.bfn;

import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.utilities.NetworkHostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class NodeRPCConnection {
    private final static Logger logger = LoggerFactory.getLogger(NodeRPCConnection.class.getSimpleName());

    // The host of the node we are connecting to.
    @Value("${config.rpc.host}")
    private String host;
    // The RPC port of the node we are connecting to.
    @Value("${config.rpc.username}")
    private String username;
    // The username for logging into the RPC client.
    @Value("${config.rpc.password}")
    private String password;
    // The password for logging into the RPC client.
    @Value("${config.rpc.port}")
    private int rpcPort;

    private CordaRPCConnection rpcConnection;
    private CordaRPCOps proxy;


    @PostConstruct
    public void initialiseNodeRPCConnection() {
        NetworkHostAndPort rpcAddress = new NetworkHostAndPort(host, rpcPort);
        CordaRPCClient rpcClient = new CordaRPCClient(rpcAddress);
        rpcConnection = rpcClient.start(username, password);
        proxy = rpcConnection.getProxy();
        NodeInfo node = proxy.nodeInfo();
        logger.info("\n\n\uD83E\uDD61 \uD83E\uDD61 \uD83E\uDD61 This node is : \uD83C\uDF4E \uD83C\uDF4E "
                .concat(node.getLegalIdentities().get(0).toString())
        .concat(" \uD83C\uDF4E \uD83C\uDF4E \uD83E\uDD61 \uD83E\uDD61 \uD83E\uDD61\n"));
        logger.info("\uD83C\uDF4E \uD83C\uDF4E host:".concat(host).concat(" \uD83C\uDF4E ").concat(" port:" + rpcPort)
        .concat(" \uD83C\uDF4B user: ").concat(username).concat(" \uD83D\uDD10 password: ")
        .concat(password).concat(" \uD83D\uDD10 \uD83D\uDD10"));
        logger.info("\uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C initialiseNodeRPCConnection \uD83E\uDD6C "
        .concat("\uD83C\uDF0D \uD83C\uDF0D Corda PlatformVersion: "
                .concat("" + node.getPlatformVersion())).concat(" \uD83C\uDF0D \uD83C\uDF0D"));
        try {
            if (host.contains("localhost")) {
                logger.info("\uD83D\uDC38 \uD83D\uDC38 \uD83D\uDC38 \uD83D\uDC38 " +
                        "We are playing close to HOME! Node is in the backyard,  \uD83D\uDC38 \uD83D\uDC38 localhost it is ... \uD83D\uDC38 \uD83D\uDC38");
            } else {
                logger.info(("\uD83D\uDCA7 \uD83D\uDCA7 \uD83D\uDCA7 \uD83D\uDCA7 " +
                        "\uD83D\uDC38 \uD83D\uDC38 \uD83D\uDC38 \uD83D\uDC38 \uD83D\uDC38 " +
                        "Hey, Toto, we are not in Kansas no more!  \uD83D\uDC38 \uD83D\uDC38 ").concat(host).concat(" \uD83D\uDCA7 \uD83D\uDCA7"));
            }
            logger.info("\uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 node host address: \uD83E\uDD6C \uD83E\uDD6C \uD83E\uDD6C \uD83D\uDE21 " +
                    "" + InetAddress.getLocalHost() + " \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21 \uD83D\uDE21");
            int cnt = 0;
            for (NodeInfo nodeInfo : proxy.networkMapSnapshot()) {
                cnt++;
                logger.info("\uD83C\uDFB2 \uD83C\uDFB2 Network Node: \uD83C\uDF0D \uD83C\uDF0D \uD83C\uDF0D \uD83E\uDD41 #"
                        .concat("" + cnt + " \uD83E\uDD41 \uD83C\uDF0D \uD83C\uDF0D \uD83C\uDF0D ").concat(nodeInfo.getLegalIdentities()
                                .get(0).getName().toString()).concat(" \uD83E\uDD41"));
            }
            cnt = 0;
            for (String registeredFlow : proxy.registeredFlows()) {
                cnt++;
                logger.info("\uD83D\uDCDF \uD83D\uDCDF \uD83D\uDCA7 RegisteredFlow: "
                        .concat("  \uD83D\uDE21 #" + cnt + " \uD83D\uDE21 ")
                        .concat(registeredFlow).concat(" \uD83D\uDCA7"));
            }
            logger.info("\uD83D\uDCDF \uD83D\uDCDF \uD83D\uDCA7 RegisteredFlows:  \uD83C\uDFB2 " + cnt + " \uD83C\uDFB2");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void close() {
//        rpcConnection.notifyServerAndClose();
    }

//    public CordaRPCOps getProxy() {
//        return proxy;
//    }
}
