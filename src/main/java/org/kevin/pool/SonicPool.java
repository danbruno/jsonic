package org.kevin.pool;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.kevin.SonicChannel;
import org.kevin.handler.AuthHandler;
import org.kevin.handler.SonicHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public final class SonicPool extends FixedChannelPool {

    private static final Logger LOG = LoggerFactory.getLogger(SonicPool.class);

    public SonicPool(Bootstrap bootstrap,
              long readTimeout,
              long idleTimeout,
              int maxConnPerHost,
              String password,
              SonicChannel sonicChannel) {
        super(bootstrap, new SonicPoolHandler(readTimeout, idleTimeout, sonicChannel, password), maxConnPerHost);
    }

    private static class SonicPoolHandler implements ChannelPoolHandler {
        final long readTimeout;
        final long idleTimeout;
        final SonicChannel channel;
        final String password;

        SonicPoolHandler(long readTimeout, long idleTimeout, SonicChannel channel, String password) {
            this.readTimeout = readTimeout;
            this.idleTimeout = idleTimeout;
            this.channel = channel;
            this.password = password;
        }

        public void channelReleased(Channel channel) throws Exception {
            if (LOG.isDebugEnabled()) {
                LOG.debug("channel released : {}", channel.toString());
            }

            channel.pipeline().get(SonicHandler.class).operation(null);
        }

        public void channelAcquired(Channel channel) throws Exception {
            if (LOG.isDebugEnabled()) {
                LOG.debug("channel acquired : {}", channel.toString());
            }

            channel.pipeline().get(SonicHandler.class).operation(null);
        }

        public void channelCreated(Channel channel) throws Exception {
            if (LOG.isInfoEnabled()) {
                LOG.info("channel created : {}", channel.toString());
            }

            ChannelPipeline pipeline = channel.pipeline();
            pipeline.addLast(new IdleStateHandler(readTimeout, 0, idleTimeout, TimeUnit.MILLISECONDS));
            pipeline.addLast(new LineBasedFrameDecoder(1024))
                    .addLast(new StringDecoder())
                    .addLast(new AuthHandler(password, SonicChannel.SEARCH))
                    .addLast(new SonicHandler());
        }
    }
}
