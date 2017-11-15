package org.openhab.binding.isy.handler;

import org.eclipse.smarthome.core.thing.Thing;
import org.openhab.binding.isy.IsyBindingConstants;
import org.openhab.binding.isy.handler.special.VenstarThermostatDeviceHandler;

public class IsyHandlerBuilder {

    IsyDeviceHandler handler;

    protected IsyHandlerBuilder(Thing thing) {
        // for thermostat, return a specialized handler based on type UID
        if (IsyBindingConstants.VENSTAR_THERMOSTAT_THING_TYPE.equals(thing.getThingTypeUID())) {
            this.handler = new VenstarThermostatDeviceHandler(thing);
        } else {
            this.handler = new IsyDeviceHandler(thing);
        }
    }

    public static IsyHandlerBuilder builder(Thing thing) {
        return new IsyHandlerBuilder(thing);
    }

    public IsyHandlerBuilder addChannelforDeviceId(String channel, int deviceId) {
        this.handler.addChannelToDevice(channel, deviceId);
        return this;
    }

    public IsyHandlerBuilder addControlChannel(String channel) {
        this.handler.setControlChannel(channel);
        return this;
    }

    public IsyDeviceHandler build() {
        return this.handler;
    }

}
