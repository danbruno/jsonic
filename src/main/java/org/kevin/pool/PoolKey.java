package org.kevin.pool;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.kevin.SonicChannel;

import java.net.InetSocketAddress;

@Getter
@EqualsAndHashCode
@ToString
public class PoolKey {
    private final InetSocketAddress addr;
    private final SonicChannel channel;

    public PoolKey(InetSocketAddress addr, SonicChannel channel){
        this.addr = addr;
        this.channel = channel;
    }
}
