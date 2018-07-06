/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.tcp_forward;

import java.io.IOException;

import org.apache.sshd.common.forward.PortForwardingEventListener;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jared Wiltshire
 */
public class LoggingPortForwardingEventListener implements PortForwardingEventListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void establishingExplicitTunnel(Session session, SshdSocketAddress local, SshdSocketAddress remote, boolean localForwarding)
            throws IOException {
        this.logger.info("session {}, local {}, remote {}, localForwarding {}", session, local, remote, localForwarding);
    }

    @Override
    public void establishedExplicitTunnel(Session session, SshdSocketAddress local, SshdSocketAddress remote, boolean localForwarding,
            SshdSocketAddress boundAddress, Throwable reason) throws IOException {
        this.logger.info("session {}, local {}, remote {}, localForwarding {}, boundAddress {}, reason {}", session, local, remote, localForwarding,
                boundAddress, reason);
    }

    @Override
    public void tearingDownExplicitTunnel(Session session, SshdSocketAddress address, boolean localForwarding) throws IOException {
        this.logger.info("session {}, address {}, localForwarding {}", session, address, localForwarding);
    }

    @Override
    public void tornDownExplicitTunnel(Session session, SshdSocketAddress address, boolean localForwarding, Throwable reason) throws IOException {
        this.logger.info("session {}, address {}, localForwarding {}, reason {}", session, address, localForwarding, reason);
    }
}