/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.russound.rnet.internal;

import com.google.common.base.Objects;

public class ZoneId {

    private int controllerId;
    private int zoneId;

    public ZoneId(int controller, int zone) {
        this.controllerId = controller;
        this.zoneId = zone;
    }

    public int getControllerId() {
        return controllerId;
    }

    public int getZoneId() {
        return zoneId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ZoneId other = (ZoneId) obj;
        return Objects.equal(this.controllerId, other.controllerId) && Objects.equal(this.zoneId, other.zoneId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.controllerId, this.zoneId);
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return super.toString();
    }

}
