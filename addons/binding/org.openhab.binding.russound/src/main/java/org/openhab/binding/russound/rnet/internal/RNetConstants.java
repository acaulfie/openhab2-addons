/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.russound.rnet.internal;

import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.russound.RussoundBindingConstants;

public class RNetConstants {
    public final static ThingTypeUID BRIDGE_TYPE_RNET = new ThingTypeUID(RussoundBindingConstants.BINDING_ID, "rnet");
    public final static ThingTypeUID BRIDGE_TYPE__RNET_CONTROLLER = new ThingTypeUID(
            RussoundBindingConstants.BINDING_ID, "rnetcontroller");
    public final static ThingTypeUID THING_TYPE_RNET_ZONE = new ThingTypeUID(RussoundBindingConstants.BINDING_ID,
            "rnetzone");

    public static final String CHANNEL_ZONESOURCE = "source";
    public static final String CHANNEL_ZONEVOLUME = "volume";
    public static final String CHANNEL_ZONESTATUS = "status";
    public static final String CHANNEL_ZONEMUTE = "mute";
}