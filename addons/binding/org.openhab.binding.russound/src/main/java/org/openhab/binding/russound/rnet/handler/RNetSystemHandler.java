/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.russound.rnet.handler;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.russound.internal.discovery.RioSystemDeviceDiscoveryService;
import org.openhab.binding.russound.internal.net.SocketSession;
import org.openhab.binding.russound.internal.rio.system.RioSystemConfig;
import org.openhab.binding.russound.internal.rio.system.RioSystemHandler;
import org.openhab.binding.russound.rnet.internal.BusParser;
import org.openhab.binding.russound.rnet.internal.PowerChangeParser;
import org.openhab.binding.russound.rnet.internal.RNetConstants;
import org.openhab.binding.russound.rnet.internal.RNetProtocolCommands;
import org.openhab.binding.russound.rnet.internal.RNetProtocolCommands.ZoneCommand;
import org.openhab.binding.russound.rnet.internal.RNetSystemConfig;
import org.openhab.binding.russound.rnet.internal.SourceChangeParser;
import org.openhab.binding.russound.rnet.internal.VolumeChangeParser;
import org.openhab.binding.russound.rnet.internal.ZoneId;
import org.openhab.binding.russound.rnet.internal.ZoneInfoParser;
import org.openhab.binding.russound.rnet.internal.ZoneStateUpdate;
import org.openhab.binding.russound.rnet.internal.connection.ConnectionProvider;
import org.openhab.binding.russound.rnet.internal.connection.ConnectionStateListener;
import org.openhab.binding.russound.rnet.internal.connection.DeviceConnection;
import org.openhab.binding.russound.rnet.internal.connection.InputHander;
import org.openhab.binding.russound.rnet.internal.connection.NoConnectionException;
import org.openhab.binding.russound.rnet.internal.connection.RNetInputStreamParser;
import org.openhab.binding.russound.rnet.internal.connection.SerialConnectionProvider;
import org.openhab.binding.russound.rnet.internal.connection.TcpConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The bridge handler for a Russound System. This is the entry point into the whole russound system and is generally
 * points to the main controller. This implementation must be attached to a {@link RioSystemHandler} bridge.
 *
 * @author Craig Hamilton
 */
public class RNetSystemHandler extends BaseBridgeHandler {
    // Logger
    private final Logger logger = LoggerFactory.getLogger(RNetSystemHandler.class);

    /**
     * The configuration for the system - will be recreated when the configuration changes and will be null when not
     * online
     */
    private RNetSystemConfig config;
    private Map<ZoneId, Thing> zones = new HashMap<ZoneId, Thing>();
    /**
     * These bus parser are responsible for examining a message and letting us know if they denote a BusAction
     */
    private Set<BusParser> busParsers = new HashSet<BusParser>();
    /**
     * The lock used to control access to {@link #config}
     */
    private final ReentrantLock configLock = new ReentrantLock();

    /**
     * The {@link SocketSession} telnet session to the switch. Will be null if not connected.
     */
    private DeviceConnection session;

    /**
     * The lock used to control access to {@link #session}
     */
    private final ReentrantLock sessionLock = new ReentrantLock();

    /**
     * The retry connection event - will only be created when we are retrying the connection attempt
     */
    private ScheduledFuture<?> retryConnection;

    /**
     * The lock used to control access to {@link #retryConnection}
     */
    private final ReentrantLock retryConnectionLock = new ReentrantLock();

    /**
     * The ping event - will be non-null when online (null otherwise)
     */
    private ScheduledFuture<?> ping;

    /**
     * The lock used to control access to {@link #ping}
     */
    private final ReentrantLock pingLock = new ReentrantLock();

    /**
     * The discovery service to discover the zones/sources, etc
     * Will be null if not active.
     */
    private final AtomicReference<RioSystemDeviceDiscoveryService> discoveryService = new AtomicReference<RioSystemDeviceDiscoveryService>(
            null);

    /**
     * Constructs the handler from the {@link Bridge}
     *
     * @param bridge a non-null {@link Bridge} the handler is for
     */
    public RNetSystemHandler(Bridge bridge) {
        super(bridge);
        busParsers.add(new VolumeChangeParser());
        busParsers.add(new PowerChangeParser());
        busParsers.add(new SourceChangeParser());
        busParsers.add(new ZoneInfoParser());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        if (command instanceof RefreshType) {
            return;
        }
        String id = channelUID.getId();
        switch (channelUID.getId()) {
            case RNetConstants.CHANNEL_SYSALLON:
                if (command instanceof OnOffType && OnOffType.ON.equals(command)) {
                    sendCommand(
                            RNetProtocolCommands.getCommand(ZoneCommand.ALLONOFF_SET, new ZoneId(0, 0), (byte) 0x01));
                } else {
                    logger.debug("Received a ZONE STATUS channel command with a non OnOffType: {}", command);
                }
                break;
            case RNetConstants.CHANNEL_SYSALLOFF:
                if (command instanceof OnOffType && OnOffType.ON.equals(command)) {
                    sendCommand(
                            RNetProtocolCommands.getCommand(ZoneCommand.ALLONOFF_SET, new ZoneId(0, 0), (byte) 0x00));
                } else {
                    logger.debug("Received a ZONE STATUS channel command with a non OnOffType: {}", command);
                }
        }
    }

    /**
     * {@inheritDoc}
     *
     * Initializes the handler. This initialization will read/validate the configuration, then will create the
     * {@link SocketSession} and will attempt to connect via {@link #connect()}.
     */
    @Override
    public void initialize() {
        final RNetSystemConfig rnetConfig = getRNetConfig();

        if (rnetConfig == null) {
            return;
        }

        if (rnetConfig.getConnectionString() == null || rnetConfig.getConnectionString().trim().length() == 0) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Connection string to Russound is missing from configuration");
            return;
        }

        sessionLock.lock();
        RNetInputStreamParser streamParser = new RNetInputStreamParser(new InputHander() {

            @Override
            public void handle(Byte[] bytes) {
                for (BusParser parser : busParsers) {
                    if (parser.matches(bytes)) {
                        ZoneStateUpdate updates = parser.process(bytes);
                        Thing zone = zones.get(updates.getZoneId());
                        if (zone != null) {
                            ((RNetZoneHandler) zone.getHandler()).processUpdates(updates.getStateUpdates());
                        }
                    }
                }

            }
        });
        // lets pick between tcp or serial. by convention if connection address starts with /tcp/ then we will be using
        // tcp
        ConnectionProvider connectionProvider;
        if (rnetConfig.getConnectionString().startsWith("/tcp/")) {
            String address = rnetConfig.getConnectionString().substring(5);
            String[] addressParts = address.split(":");
            connectionProvider = new TcpConnectionProvider(addressParts[0], Integer.parseInt(addressParts[1]));

        } else {
            connectionProvider = new SerialConnectionProvider(rnetConfig.getConnectionString());
        }

        try {
            session = new DeviceConnection(connectionProvider, streamParser);
            session.setConnectionStateListener(new ConnectionStateListener() {

                @Override
                public void isConnected(boolean value) {
                    logger.debug("received connection notification: {}", value);
                    if (value) {
                        updateStatus(ThingStatus.ONLINE);
                    } else {
                        updateStatus(ThingStatus.OFFLINE);
                    }
                }
            });
        } finally {
            sessionLock.unlock();
        }

        // Try initial connection in a scheduled task
        this.scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                connect();
            }

        }, 10, TimeUnit.MILLISECONDS);
    }

    /**
     * Attempts to connect to the system. If successfully connect, the {@link RioSystemProtocol#login()} will be
     * called to log into the system (if needed). Once completed, a ping job will be created to keep the connection
     * alive. If a connection cannot be established (or login failed), the connection attempt will be retried later (via
     * {@link #retryConnect()})
     */
    private void connect() {
        String response = "Server is offline - will try to reconnect later";

        sessionLock.lock();
        pingLock.lock();
        try {
            session.connect();

        } catch (Exception e) {
            logger.error("Error connecting: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, response);
            reconnect();
            // do nothing
        } finally {
            pingLock.unlock();
            sessionLock.unlock();
        }

    }

    /**
     * Retries the connection attempt - schedules a job in {@link RioSystemConfig#getRetryPolling()} seconds to
     * call the {@link #connect()} method. If a retry attempt is pending, the request is ignored.
     */
    protected void reconnect() {
        retryConnectionLock.lock();
        try {
            if (retryConnection == null) {
                final RNetSystemConfig rnetConfig = getRNetConfig();
                if (rnetConfig != null) {

                    logger.info("Will try to reconnect in {} seconds", rnetConfig.getRetryPolling());
                    retryConnection = this.scheduler.schedule(new Runnable() {
                        @Override
                        public void run() {
                            retryConnection = null;
                            try {
                                if (getThing().getStatus() != ThingStatus.ONLINE) {
                                    connect();
                                }
                            } catch (Exception e) {
                                logger.error("Exception connecting: {}", e.getMessage(), e);
                            }
                        }

                    }, rnetConfig.getRetryPolling(), TimeUnit.SECONDS);
                }
            } else {
                logger.debug("RetryConnection called when a retry connection is pending - ignoring request");
            }
        } finally {
            retryConnectionLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Attempts to disconnect from the session. The protocol handler will be set to null, the {@link #ping} will be
     * cancelled/set to null and the {@link #session} will be disconnected
     */
    protected void disconnect() {
        // Cancel ping
        pingLock.lock();
        try {
            if (ping != null) {
                ping.cancel(true);
                ping = null;
            }
        } finally {
            pingLock.unlock();
        }
        sessionLock.lock();
        try {
            session.disconnect();
        } finally {
            sessionLock.unlock();
        }
    }

    /**
     * Simple gets the {@link RioSystemConfig} from the {@link Thing} and will set the status to offline if not
     * found.
     *
     * @return a possible null {@link RioSystemConfig}
     */
    public RNetSystemConfig getRNetConfig() {
        configLock.lock();
        try {
            final RNetSystemConfig sysConfig = getThing().getConfiguration().as(RNetSystemConfig.class);

            if (sysConfig == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Configuration file missing");
            } else {
                config = sysConfig;
            }
            return config;
        } finally {
            configLock.unlock();
        }
    }

    /**
     * Registers the {@link RioSystemDeviceDiscoveryService} with this handler. The discovery service will be called in
     * {@link #startScan(RioSystemConfig)} when a device should be scanned and 'things' discovered from it
     *
     * @param service a possibly null {@link RioSystemDeviceDiscoveryService}
     */
    public void registerDiscoveryService(RioSystemDeviceDiscoveryService service) {
        discoveryService.set(service);
    }

    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        // TODO Auto-generated method stub
        super.handleConfigurationUpdate(configurationParameters);
    }

    /**
     * Helper method to possibly start a scan. A scan will ONLY be started if the {@link RioSystemConfig#isScanDevice()}
     * is true and a discovery service has been set ({@link #registerDiscoveryService(RioSystemDeviceDiscoveryService)})
     *
     * @param sysConfig a non-null {@link RioSystemConfig}
     */
    private void startScan(RioSystemConfig sysConfig) {
        final RioSystemDeviceDiscoveryService service = discoveryService.get();
        if (service != null) {
            if (sysConfig != null && sysConfig.isScanDevice()) {
                this.scheduler.execute(new Runnable() {
                    @Override
                    public void run() {
                        logger.info("Starting device discovery");
                        service.scanDevice();
                    }
                });
            }
        }
    }

    private ZoneId zoneIdFromThing(Thing childThing) {
        if (!childThing.getConfiguration().getProperties().containsKey("controller")) {
            throw new IllegalArgumentException("childThing does not have required 'controller' property");
        }
        if (!childThing.getConfiguration().getProperties().containsKey("zone")) {
            throw new IllegalArgumentException("childThing does not have required 'zone' property");
        }
        int zone = ((BigDecimal) childThing.getConfiguration().getProperties().get("zone")).intValue();
        int controller = ((BigDecimal) childThing.getConfiguration().getProperties().get("controller")).intValue();
        return new ZoneId(controller, zone);

    }

    /**
     * Overrides the base to call {@link #childChanged(ThingHandler)} to recreate the sources/controllers names
     */
    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        // childChanged(childHandler, true);
        logger.debug("child handler initialized, child: {}", childThing);
        try {
            zones.put(zoneIdFromThing(childThing), childThing);
        } catch (IllegalArgumentException e) {
            logger.error("Configuration error, childThing expected to have controller and zone field", e);
        }
    }

    /**
     * Overrides the base to call {@link #childChanged(ThingHandler)} to recreate the sources/controllers names
     */
    @Override
    public void childHandlerDisposed(ThingHandler childHandler, Thing childThing) {
        try {
            zones.remove(zoneIdFromThing(childThing));
        } catch (IllegalArgumentException e) {
            logger.error("Configuration error, childThing expected to have controller and zone field", e);
        }
    }

    public void sendCommand(Byte[] command) {
        try {
            session.sendCommand(ArrayUtils.toPrimitive(addChecksumandTerminator(command)));
        } catch (NoConnectionException e) {
            logger.debug("received no connection exception", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }

    }

    private Byte[] addChecksumandTerminator(Byte[] command) {
        Byte[] commandWithChecksumandTerminator = Arrays.copyOf(command, command.length + 2);
        commandWithChecksumandTerminator[commandWithChecksumandTerminator.length - 2] = russChecksum(command);
        commandWithChecksumandTerminator[commandWithChecksumandTerminator.length - 1] = (byte) 0xf7;
        return commandWithChecksumandTerminator;
    }

    private byte russChecksum(Byte[] data) {
        int sum = 0;
        for (int i = 0; i < data.length; i++) {
            sum = sum + data[i];
        }
        sum = sum + data.length;
        byte checksum = (byte) (sum & 0x007F);
        return checksum;
    }
}
