package net.jsonic.codec;

import net.jsonic.exchange.Replier;

import java.util.Optional;

public class ResultDecoder implements Replier.Decoder<Long> {
    @Override
    public Long decode(String buf) {
        return Optional.ofNullable(buf)
                .map(e -> {
                    String[] arr = e.split(" ");
                    if(arr.length > 1){
                        return Long.parseLong(arr[1]);
                    }
                    return 0L;
                }).orElse(0L);

    }
}
