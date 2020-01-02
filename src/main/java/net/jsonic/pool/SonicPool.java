package net.jsonic.pool;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import net.jsonic.SonicChannelType;
import net.jsonic.handler.AuthHandler;
import net.jsonic.handler.SonicHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

@Slf4j
public final class SonicPool extends FixedChannelPool {

    public SonicPool(Bootstrap bootstrap,
              long readTimeout,
              long idleTimeout,
              int maxConnPerHost,
              String password,
              SonicChannelType sonicChannel) {
        super(bootstrap, new SonicPoolHandler(readTimeout, idleTimeout, sonicChannel, password), maxConnPerHost);
    }

    private static class SonicPoolHandler implements ChannelPoolHandler {
        final long readTimeout;
        final long idleTimeout;
        final SonicChannelType channel;
        final String password;

        SonicPoolHandler(long readTimeout, long idleTimeout, SonicChannelType channel, String password) {
            this.readTimeout = readTimeout;
            this.idleTimeout = idleTimeout;
            this.channel = channel;
            this.password = password;
        }

        public void channelReleased(Channel channel) throws Exception {
            if (log.isDebugEnabled()) {
                log.debug("channel released : {}", channel.toString());
            }

            channel.pipeline().get(SonicHandler.class).setOperation(null);
        }

        public void channelAcquired(Channel channel) throws Exception {
            if (log.isDebugEnabled()) {
                log.debug("channel acquired : {}", channel.toString());
            }

            channel.pipeline().get(SonicHandler.class).setOperation(null);
        }

        public void channelCreated(Channel channel) throws Exception {
            if (log.isInfoEnabled()) {
                log.info("channel created : {}", channel.toString());
            }

            ChannelPipeline pipeline = channel.pipeline();
            pipeline.addLast(new IdleStateHandler(readTimeout, 0, idleTimeout, TimeUnit.MILLISECONDS));
            pipeline.addLast(new LineBasedFrameDecoder(1024))
                    .addLast(new StringDecoder())
                    .addLast(new AuthHandler(password, SonicChannelType.SEARCH))
                    .addLast(new SonicHandler());
        }
    }
}
