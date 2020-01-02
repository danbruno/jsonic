package net.jsonic.pool;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.jsonic.SonicChannelType;

import java.net.InetSocketAddress;

@Getter
@EqualsAndHashCode
@ToString
public class PoolKey {
    private final InetSocketAddress addr;
    private final SonicChannelType channel;

    public PoolKey(InetSocketAddress addr, SonicChannelType channel){
        this.addr = addr;
        this.channel = channel;
    }
}
