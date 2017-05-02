package org.openhab.binding.russound.rnet.internal;

import org.eclipse.smarthome.core.library.types.PercentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VolumeChangeParser implements BusParser {
    private static final Logger logger = LoggerFactory.getLogger(VolumeChangeParser.class);

    // controller is 1, zone is 2 (subtract 1)
    public VolumeChangeParser() {
    }

    @Override
    public boolean matches(Byte[] bytes) {
        if (bytes[0] == (byte) 0xF0 && bytes[10] == (byte) 0xf1 && bytes[11] == (byte) 0x6f) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public ZoneStateUpdate process(Byte[] bytes) {
        if (matches(bytes)) {
            int volume = bytes[8] * 2;
            logger.debug("Volume detected: {}, controller: {}, zone: {}", volume, bytes[1] + 1, bytes[2] + 1);
            return new ZoneStateUpdate(new ZoneId(bytes[1] + 1, bytes[2] + 1), RNetConstants.CHANNEL_ZONEVOLUME,
                    new PercentType(volume));
        } else {
            return null;
        }
    }

    // public void parse(byte[] bytes, AudioZone zone) {
    // zone.setVolume(bytes[8] * 2);
    // }

}