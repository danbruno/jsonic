/**
 *
 */
package net.jsonic.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.jsonic.exception.SonicException;
import net.jsonic.exception.SonicReadTimeoutException;
import net.jsonic.exception.SonicTimeoutException;
import net.jsonic.SonicOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

@Slf4j
public final class SonicHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(SonicHandler.class);

    @Setter
    private volatile SonicOperation<?> operation;

    private static final String QUERY = "EVENT QUERY";
    private static final String SUGGEST = "EVENT SUGGEST";
    private static final String OK = "OK";
    private static final String ERR = "ERR";
    private static final String RESULT = "RESULT";
    private static final String PONG = "PONG";


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("Read " + msg + " on channel " + ctx.channel());

        String resp = (String) msg;
        if(resp != null){
            if(resp.startsWith(QUERY)
                    || resp.startsWith(OK)
                    || resp.startsWith(SUGGEST)
                    || resp.startsWith(RESULT)
                    || resp.startsWith(PONG)){
                operation.await(resp);
            }else if(resp.startsWith(ERR)){
                operation.caught(new SonicException(resp));
            }else{
                super.channelRead(ctx, msg);
            }
        }
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        // read idle event.
        if (evt == IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT
                || evt == IdleStateEvent.READER_IDLE_STATE_EVENT) {

            if (null != operation) {
                throw new SonicReadTimeoutException(
                        String.format(
                                "execute %s read timeout.",
                                operation
                        )
                );
            }

            return;
        }

        // all idle event.
        if (evt == IdleStateEvent.FIRST_ALL_IDLE_STATE_EVENT
                || evt == IdleStateEvent.ALL_IDLE_STATE_EVENT) {
            throw new SonicTimeoutException("sonic channel " + ctx.channel().toString() + " was idle timeout.");
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        ctx.close();

        if (operation != null && !operation.isDone()) {
            throw new SonicException("channel " + ctx.channel().toString() + " closed.");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();

        Throwable error = translateException(cause);

        if (null != operation) {
            operation.caught(error);
            return;
        }

        // idle timeout.
        if (error instanceof SonicTimeoutException) {
            LOG.debug(error.getMessage(), error);
            return;
        }

        LOG.error(error.getMessage(), error);
    }

    private Throwable translateException(Throwable cause) {
        if (cause instanceof SonicException) {
            return cause;
        }

        Throwable unwrap = cause;
        for (; ; ) {

            if (unwrap instanceof InvocationTargetException) {
                unwrap = ((InvocationTargetException) unwrap).getTargetException();
                continue;
            }

            if (unwrap instanceof UndeclaredThrowableException) {
                unwrap = ((UndeclaredThrowableException) unwrap).getUndeclaredThrowable();
                continue;
            }

            break;
        }

        return new SonicException("sonic operation error.", unwrap);
    }
}
