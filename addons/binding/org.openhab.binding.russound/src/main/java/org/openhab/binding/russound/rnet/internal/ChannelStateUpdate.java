package org.openhab.binding.russound.rnet.internal;

import org.eclipse.smarthome.core.types.State;

public class ChannelStateUpdate {

    private String channel;
    private State state;

    public String getChannel() {
        return channel;
    }

    public State getState() {
        return state;
    }

    public ChannelStateUpdate(String channel, State state) {
        super();
        this.channel = channel;
        this.state = state;
    }

}
