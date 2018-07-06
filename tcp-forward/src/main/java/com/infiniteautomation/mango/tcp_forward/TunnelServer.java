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
import java.security.KeyPairGenerator;
import java.util.Arrays;
import java.util.Properties;

import org.apache.sshd.common.cipher.ECCurves;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jared Wiltshire
 */
public class TunnelServer {

    public static void main(String[] args) throws Exception {
        String configPath = args.length > 1 ? args[1] : "server-config.properties";

        Properties defaults = new Properties();
        try (InputStream is = TunnelClient.class.getClassLoader().getResourceAsStream("server-config.properties")) {
            defaults.load(is);
        }

        Properties config = new Properties(defaults);
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
    private SshServer sshd;

    public TunnelServer(Properties config) throws IOException, GeneralSecurityException {
        this.config = config;
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
        KeyPair keyPair = createKeyPair();

        //server.publicKeyAlgorithm=ECDSA
        //server.publicKey=
        //server.privateKey=

        this.sshd = SshServer.setUpDefaultServer();
        sshd.setHost(config.getProperty("server.host"));
        sshd.setPort(Integer.parseInt(config.getProperty("server.port")));
        //sshd.setShellFactory(new InteractiveProcessShellFactory());

        //PropertyResolverUtils.updateProperty(sshd, FactoryManager.WINDOW_SIZE, 2048);
        //PropertyResolverUtils.updateProperty(sshd, FactoryManager.MAX_PACKET_SIZE, "256");

        sshd.setKeyPairProvider(() -> Arrays.asList(keyPair));
        //sshd.setPasswordAuthenticator(BogusPasswordAuthenticator.INSTANCE);
        sshd.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);

        sshd.setForwardingFilter(AcceptAllForwardingFilter.INSTANCE);
        sshd.addPortForwardingEventListener(new LoggingPortForwardingEventListener());

        sshd.start();
    }

    private KeyPair createKeyPair() throws GeneralSecurityException {
        KeyPairGenerator gen = SecurityUtils.getKeyPairGenerator(KeyUtils.EC_ALGORITHM);
        gen.initialize(ECCurves.nistp521.getParameters());
        return gen.generateKeyPair();
    }

}
