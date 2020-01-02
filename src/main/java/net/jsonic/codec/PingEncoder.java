package net.jsonic.codec;

import net.jsonic.enums.Command;

public class PingEncoder extends CommandEncoder {

    public PingEncoder(){
    }
    @Override
    public void wrapperCommand(StringBuffer sb) {
        //command
        sb.append(this.getCommand());
    }

    @Override
    public String getCommand() {
        return Command.PING.name();
    }
}
