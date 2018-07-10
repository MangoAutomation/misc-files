/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.tcp_forward;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import org.apache.sshd.common.channel.RequestHandler;
import org.apache.sshd.common.session.ConnectionService;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jared Wiltshire
 */
public class TunnelServer {

    public static void main(String[] args) throws Exception {
        String configPath = args.length > 1 ? args[1] : "server-config.properties";

        Properties config = new Properties();
        File configFile = new File(configPath);
        if (configFile.exists()) {
            try (InputStream is = new FileInputStream(configFile)) {
                config.load(is);
            }
        }

        TunnelServer tunnelServer = new TunnelServer(config);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            tunnelServer.stop();
        }));

        tunnelServer.start();

        Thread.currentThread().join();
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Properties config;
    private final SshServer sshd;

    public TunnelServer(Properties config) throws IOException, GeneralSecurityException {
        this.config = config;
        this.sshd = SshServer.setUpDefaultServer();
    }

    public void stop() {
        if (this.sshd != null) {
            try {
                this.sshd.stop();
            } catch (IOException e) {
                logger.error("Failed to stop sshd", e);
            }
        }
    }

    public void start() throws IOException, GeneralSecurityException, InterruptedException {
        KeyPair keyPair = loadKeyPair();

        //server.publicKeyAlgorithm=ECDSA
        //server.publicKey=
        //server.privateKey=

        sshd.setHost(config.getProperty("bind", ""));
        sshd.setPort(Integer.parseInt(config.getProperty("port", "2222")));
        //sshd.setShellFactory(new InteractiveProcessShellFactory());

        //PropertyResolverUtils.updateProperty(sshd, FactoryManager.WINDOW_SIZE, 2048);
        //PropertyResolverUtils.updateProperty(sshd, FactoryManager.MAX_PACKET_SIZE, "256");

        sshd.setKeyPairProvider(() -> Arrays.asList(keyPair));
        //sshd.setPasswordAuthenticator(BogusPasswordAuthenticator.INSTANCE);
        sshd.setPublickeyAuthenticator(new AuthorizedKeysAuthenticator(new File(config.getProperty("authorizedKeysFile"))));

        sshd.setForwardingFilter(AcceptAllForwardingFilter.INSTANCE);
        sshd.addPortForwardingEventListener(new LoggingPortForwardingEventListener());

        ArrayList<RequestHandler<ConnectionService>> handlers = new ArrayList<RequestHandler<ConnectionService>>(sshd.getGlobalRequestHandlers());

        handlers.add(new InfoHandler());

        sshd.setGlobalRequestHandlers(handlers);

        sshd.start();
    }

    private KeyPair loadKeyPair() throws IOException, GeneralSecurityException {
        try (InputStream is = new FileInputStream(new File(config.getProperty("privateKeyFile")))) {
            return SecurityUtils.loadKeyPairIdentity("server-private-key", is, null);
        }
    }
}
