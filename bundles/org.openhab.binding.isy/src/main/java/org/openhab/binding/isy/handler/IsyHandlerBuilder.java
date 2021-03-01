package org.openhab.binding.isy.handler;

import org.openhab.core.thing.Thing;

public class IsyHandlerBuilder {

    IsyDeviceHandler handler;

    protected IsyHandlerBuilder(Thing thing) {
        this.handler = new IsyDeviceHandler(thing);
    }

    public static IsyHandlerBuilder builder(Thing thing) {
        return new IsyHandlerBuilder(thing);
    }

    public IsyHandlerBuilder addChannelforDeviceId(String channel, int deviceId) {
        this.handler.addChannelToDevice(channel, deviceId);
        return this;
    }

    public IsyHandlerBuilder addControlChannel(String channel, int deviceId) {
        this.handler.addControlChannel(channel, deviceId);
        return this;
    }

    public IsyDeviceHandler build() {
        return this.handler;
    }
}
