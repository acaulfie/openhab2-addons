package org.openhab.binding.russound.rnet.handler;

import java.io.IOException;
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
import org.openhab.binding.russound.rnet.internal.SourceChangeParser;
import org.openhab.binding.russound.rnet.internal.VolumeChangeParser;
import org.openhab.binding.russound.rnet.internal.ZoneId;
import org.openhab.binding.russound.rnet.internal.ZoneInfoParser;
import org.openhab.binding.russound.rnet.internal.ZoneStateUpdate;
import org.openhab.binding.russound.rnet.net.RNetResponseReader;
import org.openhab.binding.russound.rnet.net.RNetSession;
import org.openhab.binding.russound.rnet.net.SessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The bridge handler for a Russound System. This is the entry point into the whole russound system and is generally
 * points to the main controller. This implementation must be attached to a {@link RioSystemHandler} bridge.
 *
 * @author Tim Roberts
 */
public class RNetSystemHandler extends BaseBridgeHandler {
    // Logger
    private final Logger logger = LoggerFactory.getLogger(RNetSystemHandler.class);

    /**
     * The configuration for the system - will be recreated when the configuration changes and will be null when not
     * online
     */
    private RioSystemConfig config;
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
    private RNetSession<Byte[]> session;

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

    // private SocketSession<Byte[]> getSocketSession() {
    // sessionLock.lock();
    // try {
    // return session;
    // } finally {
    // sessionLock.unlock();
    // }
    // }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        if (command instanceof RefreshType) {
            // handleRefresh(channelUID.getId());
            return;
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
        final RioSystemConfig rioConfig = getRioConfig();

        if (rioConfig == null) {
            return;
        }

        if (rioConfig.getIpAddress() == null || rioConfig.getIpAddress().trim().length() == 0) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "IP Address of Russound is missing from configuration");
            return;
        }

        sessionLock.lock();
        try {
            session = new RNetSession<Byte[]>(rioConfig.getIpAddress(), 7777, new RNetResponseReader());
            session.addListener(new SessionListener<Byte[]>() {

                @Override
                public void responseReceived(Byte[] response) throws InterruptedException {
                    for (BusParser parser : busParsers) {
                        if (parser.matches(response)) {
                            ZoneStateUpdate updates = parser.process(response);
                            Thing zone = zones.get(updates.getZoneId());
                            if (zone != null) {
                                ((RNetZoneHandler) zone.getHandler()).processUpdates(updates.getStateUpdates());
                            }

                        }
                    }
                }

                @Override
                public void responseException(IOException e) throws InterruptedException {
                    logger.error("Received exception from session: {}", e);

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

        }, 1, TimeUnit.SECONDS);
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
            updateStatus(ThingStatus.ONLINE);
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
                final RioSystemConfig rioConfig = getRioConfig();
                if (rioConfig != null) {

                    logger.info("Will try to reconnect in {} seconds", rioConfig.getRetryPolling());
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

                    }, rioConfig.getRetryPolling(), TimeUnit.SECONDS);
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
        } catch (IOException e) {
            // ignore - we don't care
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
    public RioSystemConfig getRioConfig() {
        configLock.lock();
        try {
            final RioSystemConfig sysConfig = getThing().getConfiguration().as(RioSystemConfig.class);

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
        int zone = Integer.parseInt(childThing.getConfiguration().getProperties().get("zone").toString());
        int controller = Integer.parseInt(childThing.getConfiguration().getProperties().get("controller").toString());
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
        } catch (IOException e) {
            logger.error("Error sending command to russounds", e);
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
