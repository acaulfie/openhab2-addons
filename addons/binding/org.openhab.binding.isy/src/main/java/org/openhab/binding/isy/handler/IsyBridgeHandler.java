package org.openhab.binding.isy.handler;

import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.isy.IsyBindingConstants;
import org.openhab.binding.isy.config.IsyBridgeConfiguration;
import org.openhab.binding.isy.discovery.IsyRestDiscoveryService;
import org.openhab.binding.isy.internal.ISYModelChangeListener;
import org.openhab.binding.isy.internal.InsteonClientProvider;
import org.openhab.binding.isy.internal.IsyRestClient;
import org.openhab.binding.isy.internal.IsyWebSocketSubscription;
import org.openhab.binding.isy.internal.NodeAddress;
import org.openhab.binding.isy.internal.OHIsyClient;
import org.openhab.binding.isy.internal.protocol.Event;
import org.openhab.binding.isy.internal.protocol.EventInfo;
import org.openhab.binding.isy.internal.protocol.Properties;
import org.openhab.binding.isy.internal.protocol.Property;
import org.openhab.binding.isy.internal.protocol.elk.AreaEvent;
import org.openhab.binding.isy.internal.protocol.elk.ZoneEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public class IsyBridgeHandler extends BaseBridgeHandler implements InsteonClientProvider {
    private String testXmlVariableUpdate = "<?xml version=\"1.0\"?><Event seqnum=\"1607\" sid=\"uuid:74\"><control>_1</control><action>6</action><node></node><eventInfo><var type=\"2\" id=\"3\"><val>0</val><ts>20170718 09:16:26</ts></var></eventInfo></Event>";
    private String testXmlNodeUpdate = "<?xml version=\"1.0\"?><Event seqnum=\"1602\" sid=\"uuid:74\"><control>ST</control><action>255</action><node>28 C1 F3 1</node><eventInfo></eventInfo></Event>";
    private Logger logger = LoggerFactory.getLogger(IsyBridgeHandler.class);

    private DiscoveryService bridgeDiscoveryService;

    private IsyRestClient isyClient;

    private IsyWebSocketSubscription eventSubscriber;
    /*
     * Responsible for subscribing to isy for events
     */

    private XStream xStream;

    public IsyBridgeHandler(Bridge bridge) {
        super(bridge);

        xStream = new XStream(new StaxDriver());
        xStream.ignoreUnknownElements();
        xStream.setClassLoader(IsyRestDiscoveryService.class.getClassLoader());
        xStream.processAnnotations(new Class[] { Properties.class, Property.class, Event.class, EventInfo.class,
                ZoneEvent.class, AreaEvent.class });
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.trace("isy bridge handler called");
    }

    @Override
    public void dispose() {
        super.dispose();
        eventSubscriber.disconnect();
        eventSubscriber = null;
        // TODO must shutdown event subscription, rest calling service, and the references for discovery
    }

    private IsyVariableHandler getVariableHandler(String id) {
        logger.debug("find thing handler for address: {}", id);
        String[] idParts = id.split(" ");
        for (Thing thing : getThing().getThings()) {
            if (IsyBindingConstants.VARIABLE_THING_TYPE.equals(thing.getThingTypeUID())) {
                String theId = (String) thing.getConfiguration().get("id");
                String theType = (String) thing.getConfiguration().get("type");
                if (theType.equals(idParts[0]) && theId.equals(idParts[1])) {
                    return (IsyVariableHandler) thing.getHandler();
                }
            }
        }
        return null;
    }

    @Override
    public void initialize() {
        logger.debug("initialize called for bridge handler");
        IsyBridgeConfiguration config = getThing().getConfiguration().as(IsyBridgeConfiguration.class);

        String usernameAndPassword = config.getUser() + ":" + config.getPassword();
        String authorizationHeaderValue = "Basic "
                + java.util.Base64.getEncoder().encodeToString(usernameAndPassword.getBytes());

        eventSubscriber = new IsyWebSocketSubscription(config.getIpAddress(), authorizationHeaderValue,
                new ISYModelChangeListener() {

                    @Override
                    public void onModelChanged(String control, String action, String node) {
                        IsyThingHandler handler = null;
                        if ("_1".equals(control) && "6".equals(action)) {
                            handler = getVariableHandler(node);
                        } else if (!"".equals(node)) {
                            handler = getThingHandler(node);
                        }
                        if (handler != null) {
                            handler.handleUpdate(control, action, node);
                        }
                    }

                    @Override
                    public void onDeviceOnLine() {
                        logger.debug("Received onDeviceOnLine message");
                        updateStatus(ThingStatus.ONLINE);
                    }

                    @Override
                    public void onDeviceOffLine() {
                        logger.debug("Received onDeviceOffLine message");
                        updateStatus(ThingStatus.OFFLINE);
                    }

                }, xStream);
        eventSubscriber.connect();
        isyClient = new IsyRestClient(config.getIpAddress(), authorizationHeaderValue, xStream);
        updateStatus(ThingStatus.ONLINE);

    }

    public void registerDiscoveryService(DiscoveryService isyBridgeDiscoveryService) {
        this.bridgeDiscoveryService = isyBridgeDiscoveryService;

    }

    public void unregisterDiscoveryService() {
        this.bridgeDiscoveryService = null;

    }

    private IsyThingHandler getThingHandler(String address) {
        logger.trace("find thing handler for address: {}", address);
        if (!address.startsWith("n")) {
            String addressNoDeviceId = NodeAddress.stripDeviceId(address);
            logger.trace("Find thing for address: {}", addressNoDeviceId);
            for (Thing thing : getThing().getThings()) {
                if (!(IsyBindingConstants.PROGRAM_THING_TYPE.equals(thing.getThingTypeUID())
                        || IsyBindingConstants.VARIABLE_THING_TYPE.equals(thing.getThingTypeUID())
                        || IsyBindingConstants.SCENE_THING_TYPE.equals(thing.getThingTypeUID()))) {

                    String theAddress = (String) thing.getConfiguration().get("address");
                    if (theAddress != null) {
                        String thingsAddress = NodeAddress.stripDeviceId(theAddress);
                        if (addressNoDeviceId.equals(thingsAddress)) {
                            logger.trace("address: {}", thingsAddress);
                            return (IsyDeviceHandler) thing.getHandler();
                        }
                    }
                }
            }

            logger.debug("No thing discovered for address: {}", address);
        } else {
            logger.debug("Did not return thing handler because detected polygot node: {}", address);
        }

        return null;
    }

    @Override
    public OHIsyClient getInsteonClient() {
        return isyClient;
    }
}
