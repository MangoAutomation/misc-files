/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.tcp_forward;

import java.nio.charset.StandardCharsets;

import org.apache.sshd.common.SshConstants;
import org.apache.sshd.common.session.ConnectionService;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.helpers.AbstractConnectionServiceRequestHandler;
import org.apache.sshd.common.util.buffer.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jared Wiltshire
 */
public class InfoHandler extends AbstractConnectionServiceRequestHandler {
    public static final String REQUEST_NAME = "get-info@infiniteautomation.com";
    public static final byte[] REQUEST_NAME_BYTES = REQUEST_NAME.getBytes(StandardCharsets.US_ASCII);
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Result process(ConnectionService connectionService, String request, boolean wantReply, Buffer buffer) throws Exception {
        if (!REQUEST_NAME.equals(request)) {
            return super.process(connectionService, request, wantReply, buffer);
        }

        String message = buffer.getString();
        logger.info("Got message {}", message);

        if (wantReply) {
            Session session = connectionService.getSession();
            buffer = session.createBuffer(SshConstants.SSH_MSG_REQUEST_SUCCESS, Integer.BYTES);
            buffer.putInt(0);
            session.writePacket(buffer);
        }

        return Result.Replied;
    }
}
