package org.openhab.binding.isy.discovery;

import java.util.Set;

import org.openhab.binding.isy.IsyBindingConstants;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.thing.ThingTypeUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IsyBridgeDiscoveryService extends AbstractDiscoveryService {
    private static final Logger logger = LoggerFactory.getLogger(IsyBridgeDiscoveryService.class);
    private static final int DISCOVER_TIMEOUT_SECONDS = 30;

    public IsyBridgeDiscoveryService() {
        super(Set.of(new ThingTypeUID(IsyBindingConstants.BINDING_ID, "-")), DISCOVER_TIMEOUT_SECONDS, false);
    }

    @Override
    protected void startScan() {
        logger.debug("start scan called for isy bridge");
    }
}
