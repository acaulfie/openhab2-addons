/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.isy.handler;

import static org.openhab.binding.isy.IsyBindingConstants.*;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.ClientBuilder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.isy.discovery.IsyRestDiscoveryService;
import org.openhab.binding.isy.handler.special.VenstarThermostatDeviceHandler;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link IsyHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Craig Hamilton - Initial contribution
 */
@Component(service = { ThingHandlerFactory.class }, configurationPid = "binding.isy")
public class IsyHandlerFactory extends BaseThingHandlerFactory {

    private static final Logger logger = LoggerFactory.getLogger(IsyHandlerFactory.class);
    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_ISYBRIDGE, MOTION_THING_TYPE,
            DIMMER_THING_TYPE, LEAKDETECTOR_THING_TYPE, SWITCH_THING_TYPE, RELAY_THING_TYPE, GARAGEDOORKIT_THING_TYPE,
            KEYPAD_LINC_6_THING_TYPE, KEYPAD_LINC_5_THING_TYPE, REMOTELINC_4_THING_TYPE, REMOTELINC_8_THING_TYPE,
            INLINELINC_SWITCH_THING_TYPE, PROGRAM_THING_TYPE, VARIABLE_THING_TYPE, SCENE_THING_TYPE,
            UNRECOGNIZED_SWITCH_THING_TYPE, KEYPADLINC_8_THING_TYPE, OUTLETLINC_DIMMER_THING_TYPE,
            TRIGGERLINC_THING_TYPE, TOGGLELINC_THING_TYPE, HIDDENDOORSENSOR_THING_TYPE, OUTLETLINC_DUAL_THING_TYPE,
            FANLINC_THING_TYPE, SMOKE_DETECTOR_THING_TYPE, VENSTAR_THERMOSTAT_THING_TYPE, EZX10_RF_THING_TYPE);

    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegistrations = new HashMap<ThingUID, ServiceRegistration<?>>();

    @Reference
    private @NonNullByDefault({}) ClientBuilder injectedClientBuilder;

    // @Activate
    // public IsyHandlerFactory(final @Reference ClientBuilder clientBuilder) {
    // this.clientBuilder = clientBuilder;
    // }
    public static final String READ_TIMEOUT = "http.receive.timeout";
    public static final String CONNECT_TIMEOUT = "http.connection.timeout";

    private static final int EVENT_STREAM_CONNECT_TIMEOUT = 10000;
    private static final int EVENT_STREAM_READ_TIMEOUT = 10000;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        // TODO: can we figure this out from the things-types.xml, it seems like duplicate work.
        if (thingTypeUID.equals(PROGRAM_THING_TYPE)) {
            return new IsyProgramHandler(thing);
        } else if (thingTypeUID.equals(SCENE_THING_TYPE)) {
            return new SceneHandler(thing);
        } else if (thingTypeUID.equals(VARIABLE_THING_TYPE)) {
            return new IsyVariableHandler(thing);
        } else if (thingTypeUID.equals(OUTLETLINC_DUAL_THING_TYPE)) {
            return IsyHandlerBuilder.builder(thing).addChannelforDeviceId(CHANNEL_DUALOUTLET_TOP, 1)
                    .addChannelforDeviceId(CHANNEL_DUALOUTLET_BOTTOM, 2).build();
        } else if (thingTypeUID.equals(MOTION_THING_TYPE)) {
            return IsyHandlerBuilder.builder(thing).addChannelforDeviceId(CHANNEL_MOTION_MOTION, 1)
                    .addChannelforDeviceId(CHANNEL_MOTION_DUSK, 2).addChannelforDeviceId(CHANNEL_MOTION_BATTERY, 3)
                    .addControlChannel(CHANNEL_CONTROL_ACTION, 1).build();
        } else if (thingTypeUID.equals(HIDDENDOORSENSOR_THING_TYPE)) {
            return IsyHandlerBuilder.builder(thing).addChannelforDeviceId(CHANNEL_OPEN_SENSOR, 1)
                    .addChannelforDeviceId(CHANNEL_MOTION_BATTERY, 3).addChannelforDeviceId(CHANNEL_HEARTBEAT, 4)
                    .addControlChannel(CHANNEL_CONTROL_ACTION, 1).build();
        } else if (thingTypeUID.equals(TRIGGERLINC_THING_TYPE)) {
            return IsyHandlerBuilder.builder(thing).addChannelforDeviceId(CHANNEL_OPEN_SENSOR, 1)
                    .addChannelforDeviceId(CHANNEL_HEARTBEAT, 4).addControlChannel(CHANNEL_CONTROL_ACTION, 1).build();
        } else if (thingTypeUID.equals(LEAKDETECTOR_THING_TYPE)) {
            return IsyHandlerBuilder.builder(thing).addChannelforDeviceId(CHANNEL_LEAK_DRY, 1)
                    .addChannelforDeviceId(CHANNEL_LEAK_WET, 2).addChannelforDeviceId(CHANNEL_HEARTBEAT, 4).build();
        } else if (thingTypeUID.equals(GARAGEDOORKIT_THING_TYPE)) {
            return IsyHandlerBuilder.builder(thing).addChannelforDeviceId(CHANNEL_GARAGE_SENSOR, 1)
                    .addChannelforDeviceId(CHANNEL_GARAGE_CONTACT, 2).build();
        } else if (thingTypeUID.equals(INLINELINC_SWITCH_THING_TYPE)) {
            return IsyHandlerBuilder.builder(thing).addChannelforDeviceId(CHANNEL_SWITCH, 1).build();
        } else if (thingTypeUID.equals(OUTLETLINC_DIMMER_THING_TYPE)) {
            return IsyHandlerBuilder.builder(thing).addChannelforDeviceId(CHANNEL_SWITCH, 1).build();
        } else if (thingTypeUID.equals(SWITCH_THING_TYPE)) {
            return IsyHandlerBuilder.builder(thing).addChannelforDeviceId(CHANNEL_SWITCH, 1)
                    .addControlChannel(CHANNEL_PADDLEACTION, 1).build();
        } else if (thingTypeUID.equals(TOGGLELINC_THING_TYPE)) {
            return IsyHandlerBuilder.builder(thing).addChannelforDeviceId(CHANNEL_SWITCH, 1)
                    .addControlChannel(CHANNEL_PADDLEACTION, 1).build();
        } else if (thingTypeUID.equals(DIMMER_THING_TYPE)) {
            return IsyHandlerBuilder.builder(thing).addChannelforDeviceId(CHANNEL_DIMMERLEVEL, 1)
                    .addControlChannel(CHANNEL_PADDLEACTION, 1).build();
        } else if (thingTypeUID.equals(FANLINC_THING_TYPE)) {
            return IsyHandlerBuilder.builder(thing).addChannelforDeviceId(CHANNEL_LOAD, 1)
                    .addChannelforDeviceId(CHANNEL_LOAD2, 2).addControlChannel(CHANNEL_PADDLEACTION, 1).build();
        } else if (thingTypeUID.equals(REMOTELINC_4_THING_TYPE)) {
            return IsyHandlerBuilder.builder(thing).addChannelforDeviceId(CHANNEL_KEYPAD_LINC_A, 1)
                    .addChannelforDeviceId(CHANNEL_KEYPAD_LINC_B, 2).addChannelforDeviceId(CHANNEL_KEYPAD_LINC_C, 3)
                    .addChannelforDeviceId(CHANNEL_KEYPAD_LINC_D, 4).addControlChannel(CHANNEL_KEYPAD_LINC_ACTION_A, 1)
                    .addControlChannel(CHANNEL_KEYPAD_LINC_ACTION_B, 2)
                    .addControlChannel(CHANNEL_KEYPAD_LINC_ACTION_C, 3)
                    .addControlChannel(CHANNEL_KEYPAD_LINC_ACTION_D, 4).build();
        } else if (thingTypeUID.equals(KEYPAD_LINC_6_THING_TYPE) || thingTypeUID.equals(KEYPAD_LINC_5_THING_TYPE)) {
            return IsyHandlerBuilder.builder(thing).addChannelforDeviceId(CHANNEL_DIMMERLEVEL, 1)
                    .addChannelforDeviceId(CHANNEL_KEYPAD_LINC_A, 3).addChannelforDeviceId(CHANNEL_KEYPAD_LINC_B, 4)
                    .addChannelforDeviceId(CHANNEL_KEYPAD_LINC_C, 5).addChannelforDeviceId(CHANNEL_KEYPAD_LINC_D, 6)
                    .addControlChannel(CHANNEL_PADDLEACTION, 1).build();
        } else if (thingTypeUID.equals(REMOTELINC_8_THING_TYPE) || thingTypeUID.equals(KEYPADLINC_8_THING_TYPE)) {
            return IsyHandlerBuilder.builder(thing).addChannelforDeviceId(CHANNEL_KEYPAD_LINC_A, 1)
                    .addChannelforDeviceId(CHANNEL_KEYPAD_LINC_B, 2).addChannelforDeviceId(CHANNEL_KEYPAD_LINC_C, 3)
                    .addChannelforDeviceId(CHANNEL_KEYPAD_LINC_D, 4).addChannelforDeviceId(CHANNEL_KEYPAD_LINC_E, 5)
                    .addChannelforDeviceId(CHANNEL_KEYPAD_LINC_F, 6).addChannelforDeviceId(CHANNEL_KEYPAD_LINC_G, 7)
                    .addChannelforDeviceId(CHANNEL_KEYPAD_LINC_H, 8).addControlChannel(CHANNEL_KEYPAD_LINC_ACTION_A, 1)
                    .addControlChannel(CHANNEL_KEYPAD_LINC_ACTION_B, 2)
                    .addControlChannel(CHANNEL_KEYPAD_LINC_ACTION_C, 3)
                    .addControlChannel(CHANNEL_KEYPAD_LINC_ACTION_D, 4)
                    .addControlChannel(CHANNEL_KEYPAD_LINC_ACTION_E, 5)
                    .addControlChannel(CHANNEL_KEYPAD_LINC_ACTION_F, 6)
                    .addControlChannel(CHANNEL_KEYPAD_LINC_ACTION_G, 7)
                    .addControlChannel(CHANNEL_KEYPAD_LINC_ACTION_H, 8).build();
        } else if (thingTypeUID.equals(SMOKE_DETECTOR_THING_TYPE)) {
            return IsyHandlerBuilder.builder(thing).addChannelforDeviceId(CHANNEL_SMOKEDETECT_SMOKE, 1)
                    .addChannelforDeviceId(CHANNEL_SMOKEDETECT_CO, 2).addChannelforDeviceId(CHANNEL_SMOKEDETECT_TEST, 3)
                    .addChannelforDeviceId(CHANNEL_SMOKEDETECT_UNKNOWNMESSAGE, 4)
                    .addChannelforDeviceId(CHANNEL_SMOKEDETECT_CLEAR, 5)
                    .addChannelforDeviceId(CHANNEL_SMOKEDETECT_LOWBAT, 6)
                    .addChannelforDeviceId(CHANNEL_SMOKEDETECT_MALFUNCTION, 7).build();
        } else if (thingTypeUID.equals(VENSTAR_THERMOSTAT_THING_TYPE)) {
            return new VenstarThermostatDeviceHandler(thing);
        } else if (thingTypeUID.equals(EZX10_RF_THING_TYPE)) {
            return IsyHandlerBuilder.builder(thing).addChannelforDeviceId(CHANNEL_SWITCH, 1)
                    .addChannelforDeviceId(CHANNEL_SWITCH, 2).build();
        } else if (thingTypeUID.equals(THING_TYPE_ISYBRIDGE)) {
            IsyBridgeHandler handler = new IsyBridgeHandler((Bridge) thing, injectedClientBuilder.build());
            registerIsyBridgeDiscoveryService(handler);
            return handler;
        }

        throw new IllegalArgumentException("No handler found for thing: " + thing);
    }

    /**
     * Register the Thing Discovery Service for a bridge.
     *
     * @param isyBridgeBridgeHandler
     */
    private void registerIsyBridgeDiscoveryService(IsyBridgeHandler isyBridgeBridgeHandler) {
        IsyRestDiscoveryService discoveryService = new IsyRestDiscoveryService(isyBridgeBridgeHandler);

        ServiceRegistration<?> discoveryServiceRegistration = bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>());

        discoveryServiceRegistrations.put(isyBridgeBridgeHandler.getThing().getUID(), discoveryServiceRegistration);
        discoveryService.activate();

        logger.debug(
                "registerIsyBridgeDiscoveryService(): Bridge Handler - {}, Class Name - {}, Discovery Service - {}",
                isyBridgeBridgeHandler, DiscoveryService.class.getName(), discoveryService);
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof IsyBridgeHandler) {
            ServiceRegistration<?> serviceReg = this.discoveryServiceRegistrations
                    .get(thingHandler.getThing().getUID());
            if (serviceReg != null) {
                // remove discovery service, if bridge handler is removed
                IsyRestDiscoveryService service = (IsyRestDiscoveryService) bundleContext
                        .getService(serviceReg.getReference());
                service.deactivate();
                serviceReg.unregister();
                discoveryServiceRegistrations.remove(thingHandler.getThing().getUID());
                logger.debug("Isy discovery service removed");
            }
        }
    }
}
