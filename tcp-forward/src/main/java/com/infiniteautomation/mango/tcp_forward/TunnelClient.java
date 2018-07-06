package com.infiniteautomation.mango.tcp_forward;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.keyverifier.RequiredServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.session.ClientSession.ClientSessionEvent;
import org.apache.sshd.client.session.forward.ExplicitPortForwardingTracker;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TunnelClient {

    public static void main(String[] args) throws Exception {
        String configPath = args.length > 1 ? args[1] : "client-config.properties";

        Properties defaults = new Properties();
        try (InputStream is = TunnelClient.class.getClassLoader().getResourceAsStream("client-config.properties")) {
            defaults.load(is);
        }

        Properties config = new Properties(defaults);
        File configFile = new File(configPath);
        if (configFile.exists()) {
            try (InputStream is = new FileInputStream(configFile)) {
                config.load(is);
            }
        }

        TunnelClient tunnelClient = new TunnelClient(config);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            tunnelClient.stop();
        }));

        tunnelClient.start();
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Properties config;
    private final SshClient client;

    public TunnelClient(Properties config) throws IOException, GeneralSecurityException {
        this.config = config;
        this.client = this.initClient();
    }

    private SshClient initClient() throws IOException, GeneralSecurityException {
        SshClient client = SshClient.setUpDefaultClient();

        KeyFactory kf = SecurityUtils.getKeyFactory(config.getProperty("server.publicKeyAlgorithm"));

        // base64 encoded DER X.509 certificate for server public key
        byte[] publicKeyBytes = Base64.getDecoder().decode(config.getProperty("server.publicKey"));
        PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(publicKeyBytes));


        client.setServerKeyVerifier(new RequiredServerKeyVerifier(publicKey));
        //client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);

        //PropertyResolverUtils.updateProperty(client, FactoryManager.WINDOW_SIZE, 2048);
        //PropertyResolverUtils.updateProperty(client, FactoryManager.MAX_PACKET_SIZE, 256);
        client.setForwardingFilter(AcceptAllForwardingFilter.INSTANCE);

        client.addPortForwardingEventListener(new LoggingPortForwardingEventListener());
        client.start();
        return client;
    }

    public void start() throws IOException, GeneralSecurityException {
        KeyPair key;

        try (InputStream is = new FileInputStream(new File(config.getProperty("client.authenticationKeyLocation")))) {
            key = SecurityUtils.loadKeyPairIdentity("my-identity", is, null);
        }

        String username = config.getProperty("server.username");
        String host = config.getProperty("server.host");
        int port = Integer.parseInt(config.getProperty("server.port"));

        SshdSocketAddress localAddress = new SshdSocketAddress(config.getProperty("localAddress.host"), Integer.parseInt(config.getProperty("localAddress.port")));
        SshdSocketAddress remoteAddress = new SshdSocketAddress(config.getProperty("remoteAddress.host"), Integer.parseInt(config.getProperty("remoteAddress.port")));

        Set<ClientSessionEvent> ret;

        while (client.isStarted()) {
            try (ClientSession session = client.connect(username, host, port).verify(10, TimeUnit.SECONDS).getSession()) {
                session.addPublicKeyIdentity(key);
                session.auth().verify(10, TimeUnit.SECONDS);

                // can use in try with resources block
                ExplicitPortForwardingTracker tracker = session.createRemotePortForwardingTracker(remoteAddress, localAddress);

                logger.info("Bound address is {}", tracker.getBoundAddress());

                //session.startRemotePortForwarding(remoteAddress, localAddress);
                //session.stopRemotePortForwarding(remoteAddress);

                session.addChannelListener(new LoggingChannelListener());
                //ClientChannel channel = session.createChannel("my-type");

                ret = session.waitFor(EnumSet.of(ClientSessionEvent.CLOSED, ClientSessionEvent.TIMEOUT), 0);
            }

            logger.info("Session terminated, reason: {}", ret);
        }

        client.stop();
    }

    public void stop() {
        client.stop();
    }
}
