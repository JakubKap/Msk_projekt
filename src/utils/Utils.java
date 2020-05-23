package utils;

import hla.rti1516e.encoding.EncoderFactory;

public class Utils {
    public static byte[] intToByte(EncoderFactory encoderFactory, int value) {
        return encoderFactory.createHLAinteger32BE(value).toByteArray();
    }
}
