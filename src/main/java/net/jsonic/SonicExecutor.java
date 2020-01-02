package net.jsonic;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import net.jsonic.exchange.Replier;
import net.jsonic.exchange.ReplierSupport;
import net.jsonic.exchange.Requestor;
import net.jsonic.exchange.RequestorEncoder;
import net.jsonic.handler.AuthHandler;
import net.jsonic.pool.PoolKey;
import net.jsonic.pool.SonicPool;
import net.jsonic.pool.SonicPoolGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public class SonicExecutor implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(SonicExecutor.class);

    private final EventLoopGroup loopGroup;
    private final SonicPoolGroup poolGroup;
    private final InetSocketAddress addr;

    SonicExecutor(SonicSettings settings) {
        loopGroup = new NioEventLoopGroup(settings.getMaxThreads());
        poolGroup = new SonicPoolGroup(
                loopGroup,
                settings.getConnectTimeout(),
                settings.getReadTimeout(),
                settings.getIdleTimeout(),
                settings.getMaxConnPerHost(),
                settings.getPassword()
        );
        this.addr = new InetSocketAddress(settings.getHost(), settings.getPort());
    }

    <T> CompletableFuture<T> execute(SonicChannelType channel, Requestor.Encoder encoder, Replier.Decoder<T> decoder) {
        return execute(channel, new RequestorEncoder(encoder), new ReplierSupport<T>(decoder));
    }

    <T> CompletableFuture<T> execute(SonicChannelType channel, Requestor.Encoder encoder, Replier<T> replier) {
        return execute(channel, new RequestorEncoder(encoder), replier);
    }

    <T> CompletableFuture<T> execute(SonicChannelType channel, Requestor requestor, Replier<T> replier) {
        CompletableFuture<T> promise = new CompletableFuture<>();
        execute(channel, requestor, replier, promise);
        return promise;
    }

    private <T> void execute(SonicChannelType channel, Requestor requestor, Replier<T> replier, CompletableFuture<T> promise) {
        PoolKey poolKey = new PoolKey(addr, channel);
        SonicPool pool = poolGroup.get(poolKey);
        pool.acquire().addListener(new SonicChannelListener<>(pool, requestor, replier, promise));
    }

    public void close() throws IOException {
        if (null != poolGroup) {
            try {
                poolGroup.close();
            } catch (Exception e) {
                // ignore
            }
        }
        if (null != loopGroup) {
            loopGroup.shutdownGracefully();
        }
    }

    private static class SonicChannelListener<T> implements FutureListener<Channel> {

        final SonicPool pool;
        final Requestor requestor;
        final Replier<T> replier;
        final CompletableFuture<T> promise;

        SonicChannelListener(SonicPool pool,
                             Requestor requestor,
                             Replier<T> replier,
                             CompletableFuture<T> promise) {
            this.pool = pool;
            this.requestor = requestor;
            this.replier = replier;
            this.promise = promise;
        }

        @Override
        public void operationComplete(Future<Channel> cf) throws Exception {

            if (cf.isCancelled()) {
                promise.cancel(true);
                return;
            }

            if (!cf.isSuccess()) {
                promise.completeExceptionally(cf.cause());
                return;
            }

            Channel channel = cf.getNow();

            promise.whenComplete((result, error) -> pool.release(channel));
            channel.writeAndFlush("\r\n");

            ChannelPipeline pipeline = channel.pipeline();

            try {

                SonicOperation<T> sonicOperation = new SonicOperation<>(channel, requestor, replier, promise);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("execute {}", sonicOperation);
                }

                pipeline.get(AuthHandler.class).authenticate().get();
                sonicOperation.execute();
            } catch (Exception e) {
                promise.completeExceptionally(e);
            }
        }
    }
}
