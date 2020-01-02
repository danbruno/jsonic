package net.jsonic.codec;

import net.jsonic.enums.Command;

public class QuitEncoder extends CommandEncoder {

    public QuitEncoder(){
    }
    @Override
    public void wrapperCommand(StringBuffer sb) {
        //command
        sb.append(this.getCommand());
    }

    @Override
    public String getCommand() {
        return Command.QUIT.name();
    }
}
