package net.jsonic.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import net.jsonic.enums.Command;
import net.jsonic.SonicChannelType;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class AuthHandler extends ChannelInboundHandlerAdapter {
    private final String password;
    private final SonicChannelType channel;
    private AtomicBoolean isAuth = new AtomicBoolean(false);
    private CompletableFuture<Void> authenticatedCompleted = new CompletableFuture<>();

    public AuthHandler(String password, SonicChannelType channel){
        this.password = password;
        this.channel = channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String resp = (String) msg;
        if(resp.startsWith("CONNECTED")){
            if(!isAuth.get()){
                //auth with password
                StringBuffer sb = new StringBuffer();
                //command
                sb.append(Command.START.name());
                sb.append(" ");
                //channel
                sb.append(this.channel.name().toLowerCase());
                sb.append(" ");
                //password
                sb.append(this.password.trim());
                sb.append("\r\n");
                ByteBuf buf = ctx.alloc().buffer(sb.length());
                buf.writeBytes(sb.toString().getBytes(StandardCharsets.UTF_8));
                ctx.writeAndFlush(buf);
            }

        }else if(resp.startsWith("STARTED")){
            isAuth.compareAndSet(false, true);
            authenticatedCompleted.complete(null);
        }else{
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        isAuth.set(false);
    }

    public CompletableFuture<Void> authenticate() {
        return authenticatedCompleted;
    }
}
