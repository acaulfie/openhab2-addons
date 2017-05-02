/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.russound.rnet.discovery;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.openhab.binding.russound.internal.RussoundHandlerFactory;
import org.openhab.binding.russound.rnet.handler.RNetSystemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RNetSystemDeviceDiscoveryService extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(RNetSystemDeviceDiscoveryService.class);

    private RNetSystemHandler sysHandler;

    public RNetSystemDeviceDiscoveryService(RNetSystemHandler handler) {
        super(RussoundHandlerFactory.SUPPORTED_THING_TYPES_UIDS, 30, false);
        if (sysHandler == null) {
            throw new IllegalArgumentException("sysHandler can't be null");
        }
        this.sysHandler = handler;
    }

    @Override
    protected void startScan() {
        logger.debug("Should start scan for RNet");
        // for now, lets assume can have up to 6 controllers, and up to 6 zones in each
        for (int controllerNumber = 0; controllerNumber < 6; controllerNumber++) {
            for (int zoneNumber = 0; controllerNumber < 6; controllerNumber++) {
                // let's request zone info for each combination. If we receive a valid return message, let's add the
                // device(s)
            }
        }

    }

}
