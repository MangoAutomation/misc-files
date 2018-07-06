/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.tcp_forward;

import org.apache.sshd.common.channel.Channel;
import org.apache.sshd.common.channel.ChannelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jared Wiltshire
 */
public class LoggingChannelListener implements ChannelListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void channelInitialized(Channel channel) {
        logger.info("channelInitialized {}", channel);
    }

    @Override
    public void channelOpenSuccess(Channel channel) {
        logger.info("channelOpenSuccess {}", channel);
    }

    @Override
    public void channelOpenFailure(Channel channel, Throwable reason) {
        logger.info("channelOpenFailure {}, reason {}", channel, reason);
    }

    @Override
    public void channelStateChanged(Channel channel, String hint) {
        logger.info("channelStateChanged {}, hint {}", channel, hint);
    }

    @Override
    public void channelClosed(Channel channel, Throwable reason) {
        logger.info("channelClosed {}, reason {}", channel, reason);
    }
}