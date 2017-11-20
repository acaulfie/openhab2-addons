/**
 *
 */
package org.openhab.binding.isy.handler.special;

import org.eclipse.smarthome.core.thing.Thing;
import org.openhab.binding.isy.handler.IsyDeviceHandler;

/**
 * @author thomas hentschel
 *
 */
public class VenstarThermostatHandlerBuilder {

    private VenstarThermostatDeviceHandler handler;

    /**
     * @param thing
     */
    protected VenstarThermostatHandlerBuilder(Thing thing) {
        this.handler = new VenstarThermostatDeviceHandler(thing);
    }

    public static VenstarThermostatHandlerBuilder builder(Thing thing) {
        return new VenstarThermostatHandlerBuilder(thing);
    }

    public VenstarThermostatHandlerBuilder addChannelforDeviceId(String channel, int deviceId) {
        this.handler.addChannelToDevice(channel, deviceId);
        return this;
    }

    public VenstarThermostatHandlerBuilder addControlChannel(String channel) {
        this.handler.setControlChannel(channel);
        return this;
    }

    public IsyDeviceHandler build() {
        return this.handler;
    }
}
