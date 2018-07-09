package com.infiniteautomation.mango.tcp_forward;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.session.ClientSession.ClientSessionEvent;
import org.apache.sshd.client.session.forward.ExplicitPortForwardingTracker;
import org.apache.sshd.common.SshConstants;
import org.apache.sshd.common.util.buffer.Buffer;
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

        Path knownHostsPath = Paths.get(config.getProperty("client.knownHostsFile"));
        // if the server is not in known_hosts file, we accept it and add it to the file
        // could use RejectAllServerKeyVerifier if we dont want this behavior
        AcceptAllServerKeyVerifier delegate = AcceptAllServerKeyVerifier.INSTANCE;
        client.setServerKeyVerifier(new KnownHostsServerKeyVerifier(delegate, knownHostsPath));

        KeyPair clientPrivateKey;
        try (InputStream is = new FileInputStream(new File(config.getProperty("client.privateKeyFile")))) {
            clientPrivateKey = SecurityUtils.loadKeyPairIdentity("client-private-key", is, null);
        }
        client.setKeyPairProvider(() -> Arrays.asList(clientPrivateKey));

        //PropertyResolverUtils.updateProperty(client, FactoryManager.WINDOW_SIZE, 2048);
        //PropertyResolverUtils.updateProperty(client, FactoryManager.MAX_PACKET_SIZE, 256);
        client.setForwardingFilter(AcceptAllForwardingFilter.INSTANCE);

        client.addPortForwardingEventListener(new LoggingPortForwardingEventListener());
        client.start();
        return client;
    }

    public void start() throws IOException, GeneralSecurityException {
        String username = config.getProperty("server.username");
        String host = config.getProperty("server.host");
        int port = Integer.parseInt(config.getProperty("server.port"));

        SshdSocketAddress localAddress = new SshdSocketAddress(config.getProperty("localAddress.host"),
                Integer.parseInt(config.getProperty("localAddress.port")));
        SshdSocketAddress remoteAddress = new SshdSocketAddress(config.getProperty("remoteAddress.host"),
                Integer.parseInt(config.getProperty("remoteAddress.port")));

        Set<ClientSessionEvent> ret = null;

        while (client.isStarted()) {
            try (ClientSession session = client.connect(username, host, port).verify(10, TimeUnit.SECONDS).getSession()) {
                session.auth().verify(10, TimeUnit.SECONDS);

                String test = "send this please";
                byte[] testBytes = test.getBytes(StandardCharsets.UTF_8);

                Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_GLOBAL_REQUEST, InfoHandler.REQUEST_NAME_BYTES.length + 1 + testBytes.length);
                buffer.putBytes(InfoHandler.REQUEST_NAME_BYTES);
                buffer.putBoolean(true); // want reply
                buffer.putBytes(testBytes);

                Buffer result = session.request(InfoHandler.REQUEST_NAME, buffer, 10, TimeUnit.SECONDS);
                int intResult = result.getInt();
                logger.info("Reply is {}", intResult);


                // can use in try with resources block
                ExplicitPortForwardingTracker tracker = session.createRemotePortForwardingTracker(remoteAddress, localAddress);

                logger.info("Bound address is {}", tracker.getBoundAddress());

                //session.startRemotePortForwarding(remoteAddress, localAddress);
                //session.stopRemotePortForwarding(remoteAddress);

                session.addChannelListener(new LoggingChannelListener());
                //ClientChannel channel = session.createChannel("my-type");

                ret = session.waitFor(EnumSet.of(ClientSessionEvent.CLOSED, ClientSessionEvent.TIMEOUT), 0);
            } catch (Exception e) {
                logger.info("Connect failed", e);
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(10));
                } catch (InterruptedException e1) {
                }
            }

            logger.info("Session terminated, reason: {}", ret);
        }

        client.stop();
    }

    public void stop() {
        client.stop();
    }
}
