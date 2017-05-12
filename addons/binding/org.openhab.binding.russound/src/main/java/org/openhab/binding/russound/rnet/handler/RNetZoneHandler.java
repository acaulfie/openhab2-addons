/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.russound.rnet.handler;

import java.util.Collection;
import java.util.concurrent.Callable;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.russound.internal.rio.RioConstants;
import org.openhab.binding.russound.internal.rio.controller.RioControllerHandler;
import org.openhab.binding.russound.internal.rio.zone.RioZoneHandler;
import org.openhab.binding.russound.rnet.internal.ChannelStateUpdate;
import org.openhab.binding.russound.rnet.internal.RNetConstants;
import org.openhab.binding.russound.rnet.internal.RNetProtocolCommands;
import org.openhab.binding.russound.rnet.internal.RNetProtocolCommands.ZoneCommand;
import org.openhab.binding.russound.rnet.internal.RnetZoneConfig;
import org.openhab.binding.russound.rnet.internal.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The bridge handler for a RNet Russound Zone. A zone is the main receiving area for music.
 *
 * @author Craig Hamilton
 */
public class RNetZoneHandler extends BaseThingHandler {
    // Logger
    private final Logger logger = LoggerFactory.getLogger(RioZoneHandler.class);

    /**
     * The zone we are attached to
     */
    private ZoneId id;
    private long lastRefreshed = 0;

    /**
     * Constructs the handler from the {@link Thing}
     *
     * @param thing a non-null {@link Thing} the handler is for
     */
    public RNetZoneHandler(Thing thing) {
        super(thing);

    }

    private void requestZoneInfo() {
        long elapsed = System.currentTimeMillis() - this.lastRefreshed;
        if (elapsed > 30000) {
            lastRefreshed = System.currentTimeMillis();
            scheduler.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    logger.debug("Requesting zone info");
                    getSystemHander().sendCommand(RNetProtocolCommands.getCommand(ZoneCommand.ZONE_INFO, id, (byte) 0));

                    return null;
                }
            });
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        if (command instanceof RefreshType) {
            if (getThing().getStatus() != ThingStatus.ONLINE) {
                return;
            }
            requestZoneInfo();

            return;
        }

        String id = channelUID.getId();

        if (id == null) {
            logger.debug("Called with a null channel id - ignoring");
            return;
        }

        if (id.equals(RioConstants.CHANNEL_ZONEBASS)) {
            if (command instanceof DecimalType) {
                // getProtocolHandler().setZoneBass(((DecimalType) command).intValue());
            } else {
                logger.debug("Received a ZONE BASS channel command with a non DecimalType: {}", command);
            }

        } else if (id.equals(RioConstants.CHANNEL_ZONETREBLE)) {
            if (command instanceof DecimalType) {
                // getProtocolHandler().setZoneTreble(((DecimalType) command).intValue());
            } else {
                logger.debug("Received a ZONE TREBLE channel command with a non DecimalType: {}", command);
            }

        } else if (id.equals(RioConstants.CHANNEL_ZONEBALANCE)) {
            if (command instanceof DecimalType) {
                getSystemHander().sendCommand(RNetProtocolCommands.getCommand(ZoneCommand.BALANCE_SET, this.id,
                        (byte) (((DecimalType) command).intValue() + 10)));
                // getProtocolHandler().setZoneBalance(((DecimalType) command).intValue());
            } else {
                logger.debug("Received a ZONE BALANCE channel command with a non DecimalType: {}", command);
            }

        } else if (id.equals(RioConstants.CHANNEL_ZONETURNONVOLUME)) {
            if (command instanceof PercentType) {
                // getProtocolHandler().setZoneTurnOnVolume(((PercentType) command).intValue() / 100d);
            } else if (command instanceof DecimalType) {
                // getProtocolHandler().setZoneTurnOnVolume(((DecimalType) command).doubleValue());
            } else {
                logger.debug("Received a ZONE TURN ON VOLUME channel command with a non PercentType/DecimalType: {}",
                        command);
            }

        } else if (id.equals(RNetConstants.CHANNEL_ZONELOUDNESS)) {
            if (command instanceof OnOffType) {
                getSystemHander().sendCommand(RNetProtocolCommands.getCommand(ZoneCommand.LOUDNESS_SET, this.id,
                        command == OnOffType.ON ? (byte) 0x01 : 0));
            } else {
                logger.debug("Received a ZONE TURN ON VOLUME channel command with a non OnOffType: {}", command);
            }

        } else if (id.equals(RioConstants.CHANNEL_ZONESLEEPTIMEREMAINING)) {
            if (command instanceof DecimalType) {
                // getProtocolHandler().setZoneSleepTimeRemaining(((DecimalType) command).intValue());
            } else {
                logger.debug("Received a ZONE SLEEP TIME REMAINING channel command with a non DecimalType: {}",
                        command);
            }
        } else if (id.equals(RNetConstants.CHANNEL_ZONESOURCE)) {
            if (command instanceof DecimalType) {
                getSystemHander().sendCommand(RNetProtocolCommands.getCommand(ZoneCommand.SOURCE_SET, this.id,
                        (byte) (((DecimalType) command).intValue() - 1)));
            } else {
                logger.debug("Received a ZONE SOURCE channel command with a non DecimalType: {}", command);
            }

        } else if (id.equals(RNetConstants.CHANNEL_ZONESTATUS)) {
            if (command instanceof OnOffType) {
                getSystemHander().sendCommand(RNetProtocolCommands.getCommand(ZoneCommand.POWER_SET, this.id,
                        command == OnOffType.ON ? (byte) 0x01 : 0));
            } else {
                logger.debug("Received a ZONE STATUS channel command with a non OnOffType: {}", command);
            }
        } else if (id.equals(RioConstants.CHANNEL_ZONEPARTYMODE)) {
            if (command instanceof StringType) {
                // getProtocolHandler().setZonePartyMode(((StringType) command).toString());
            } else {
                logger.debug("Received a ZONE PARTY MODE channel command with a non StringType: {}", command);
            }

        } else if (id.equals(RioConstants.CHANNEL_ZONEDONOTDISTURB)) {
            if (command instanceof StringType) {
                // getProtocolHandler().setZoneDoNotDisturb(((StringType) command).toString());
            } else {
                logger.debug("Received a ZONE DO NOT DISTURB channel command with a non StringType: {}", command);
            }

        } else if (id.equals(RioConstants.CHANNEL_ZONEMUTE)) {
            if (command instanceof OnOffType) {
                // getProtocolHandler().toggleZoneMute();
            } else {
                logger.debug("Received a ZONE MUTE channel command with a non OnOffType: {}", command);
            }

        } else if (id.equals(RioConstants.CHANNEL_ZONEREPEAT)) {
            if (command instanceof OnOffType) {
                // getProtocolHandler().toggleZoneRepeat();
            } else {
                logger.debug("Received a ZONE REPEAT channel command with a non OnOffType: {}", command);
            }

        } else if (id.equals(RioConstants.CHANNEL_ZONESHUFFLE)) {
            if (command instanceof OnOffType) {
                // getProtocolHandler().toggleZoneShuffle();
            } else {
                logger.debug("Received a ZONE SHUFFLE channel command with a non OnOffType: {}", command);
            }

        } else if (id.equals(RioConstants.CHANNEL_ZONEVOLUME)) {
            if (command instanceof OnOffType) {
                getSystemHander().sendCommand(
                        RNetProtocolCommands.getCommand(ZoneCommand.VOLUME_SET, this.id, (byte) (100 / 2)));
            } else if (command instanceof IncreaseDecreaseType) {
                // getProtocolHandler().setZoneVolume(command == IncreaseDecreaseType.INCREASE);
            } else if (command instanceof PercentType) {
                getSystemHander().sendCommand(RNetProtocolCommands.getCommand(ZoneCommand.VOLUME_SET, this.id,
                        (byte) (((PercentType) command).intValue() / 2)));
            } else if (command instanceof DecimalType) {
                // getProtocolHandler().setZoneVolume(((DecimalType) command).doubleValue());
            } else {
                logger.debug(
                        "Received a ZONE VOLUME channel command with a non OnOffType/IncreaseDecreaseType/PercentType/DecimalTye: {}",
                        command);
            }

        } else if (id.equals(RioConstants.CHANNEL_ZONERATING)) {
            if (command instanceof OnOffType) {
                // getProtocolHandler().setZoneRating(command == OnOffType.ON);
            } else {
                logger.debug("Received a ZONE RATING channel command with a non OnOffType: {}", command);
            }

        } else if (id.equals(RioConstants.CHANNEL_ZONEKEYPRESS)) {
            if (command instanceof StringType) {
                // getProtocolHandler().sendKeyPress(((StringType) command).toString());
            } else {
                logger.debug("Received a ZONE KEYPRESS channel command with a non StringType: {}", command);
            }

        } else if (id.equals(RioConstants.CHANNEL_ZONEKEYRELEASE)) {
            if (command instanceof StringType) {
                // getProtocolHandler().sendKeyRelease(((StringType) command).toString());
            } else {
                logger.debug("Received a ZONE KEYRELEASE channel command with a non StringType: {}", command);
            }

        } else if (id.equals(RioConstants.CHANNEL_ZONEKEYHOLD)) {
            if (command instanceof StringType) {
                // getProtocolHandler().sendKeyHold(((StringType) command).toString());
            } else {
                logger.debug("Received a ZONE KEYHOLD channel command with a non StringType: {}", command);
            }

        } else if (id.equals(RioConstants.CHANNEL_ZONEKEYCODE)) {
            if (command instanceof StringType) {
                // getProtocolHandler().sendKeyCode(((StringType) command).toString());
            } else {
                logger.debug("Received a ZONE KEYCODE channel command with a non StringType: {}", command);
            }

        } else if (id.equals(RioConstants.CHANNEL_ZONEEVENT)) {
            if (command instanceof StringType) {
                // getProtocolHandler().sendEvent(((StringType) command).toString());
            } else {
                logger.debug("Received a ZONE EVENT channel command with a non StringType: {}", command);
            }

        } else if (id.equals(RioConstants.CHANNEL_ZONEMMINIT)) {
            // getProtocolHandler().sendMMInit();

        } else if (id.equals(RioConstants.CHANNEL_ZONEMMCONTEXTMENU)) {
            // getProtocolHandler().sendMMContextMenu();

        } else {
            logger.debug("Unknown/Unsupported Channel id: {}", id);
        }
    }

    /**
     * Initializes the bridge. Confirms the configuration is valid and that our parent bridge is a
     * {@link RioControllerHandler}. Once validated, a {@link RioZoneProtocol} is set via
     * {@link #setProtocolHandler(RioZoneProtocol)} and the bridge comes online.
     */
    @Override
    public void initialize() {
        final Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Cannot be initialized without a bridge");
            return;
        }
        if (bridge.getStatus() != ThingStatus.ONLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }

        final ThingHandler handler = bridge.getHandler();
        if (handler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "No handler specified (null) for the bridge!");
            return;
        }

        final RnetZoneConfig config = getThing().getConfiguration().as(RnetZoneConfig.class);
        if (config == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Configuration file missing");
            return;
        }

        final int configZone = config.getZone();
        if (configZone < 1 || configZone > 6) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Source must be between 1 and 6: " + configZone);
            return;
        }

        final int configController = config.getController();
        if (configController < 1 || configController > 6) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Controller must be between 1 and 6: " + configZone);
            return;
        }
        this.id = new ZoneId(configController, configZone);
        updateStatus(ThingStatus.ONLINE);
    }

    public void processUpdates(Collection<ChannelStateUpdate> updates) {

        for (ChannelStateUpdate update : updates) {
            // if we change power, and are doing it from a collection of size one (ie not an overall zone update) then
            // let's reload the other attributes as well and the turn on attributes kick in
            if (updates.size() == 1 && RNetConstants.CHANNEL_ZONESTATUS.equals(update.getChannel())) {
                this.lastRefreshed = 0;
                requestZoneInfo();
            }
            updateState(update.getChannel(), update.getState());
        }
    }

    private RNetSystemHandler getSystemHander() {
        return (RNetSystemHandler) this.getBridge().getHandler();
    }

}
