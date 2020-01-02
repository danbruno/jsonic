package net.jsonic.exchange;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public abstract class RequestorSupport implements Requestor {

    @Override
    public void request(Channel channel) {
        List<Object> requests = writeRequests(channel.alloc());

        requests.forEach(channel::write);
        channel.flush();
    }

    /**
     * @param alloc
     */
    protected abstract List<Object> writeRequests(ByteBufAllocator alloc);
}
