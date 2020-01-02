package org.kevin;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.kevin.exchange.Replier;
import org.kevin.exchange.Requestor;
import org.kevin.handler.SonicHandler;

import java.util.concurrent.CompletableFuture;

@Slf4j
public final class SonicOperation<T> {

    private final Channel channel;
    private final Requestor requestor;
    private final Replier<T> replier;
    private final CompletableFuture<T> promise;
    private boolean authenticated = false;

    public SonicOperation(Channel channel, Requestor requestor, Replier<T> replier, CompletableFuture<T> promise) {
        this.channel = channel;
        this.requestor = requestor;
        this.replier = replier;
        this.promise = promise;
    }

    public void execute() {
        channel.pipeline().get(SonicHandler.class).operation(this);

        try {
            requestor.request(channel);
        } catch (Exception e) {
            caught(e);
        }
    }

    public boolean isDone() {
        return promise.isDone();
    }

    public void await(String in) {
        try {
            replier.reply(in, promise);
        } catch (Exception e) {
            caught(e);
        }
    }

    public void caught(Throwable cause) {
        promise.completeExceptionally(cause);
    }

    @Override
    public String toString() {
        return "SonicOperation{" +
                "channel=" + channel +
                ", replier=" + replier +
                ", requestor=" + requestor +
                '}';
    }
}
