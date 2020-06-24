package utils;

import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAboolean;
import hla.rti1516e.encoding.HLAinteger32BE;
import org.portico.impl.hla1516e.types.encoding.HLA1516eBoolean;
import org.portico.impl.hla1516e.types.encoding.HLA1516eInteger32BE;

public class Utils {
    public static byte[] intToByte(EncoderFactory encoderFactory, int value) {
        return encoderFactory.createHLAinteger32BE(value).toByteArray();
    }

    public static int byteToInt(byte[] bytes) {
        HLAinteger32BE value = new HLA1516eInteger32BE();
        try {
            value.decode(bytes);
        } catch (DecoderException e) {
            e.printStackTrace();
        }
        return value.getValue();
    }

    public static byte[] booleanToByte(EncoderFactory encoderFactory, boolean value) {
        return encoderFactory.createHLAboolean(value).toByteArray();
    }

    public static boolean byteToBoolean(byte[] bytes) {
        HLAboolean value = new HLA1516eBoolean();
        try {
            value.decode(bytes);
        } catch (DecoderException e) {
            e.printStackTrace();
        }
        return value.getValue();
    }
}
