package utils;

import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ParameterHandle;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import org.portico.impl.hla1516e.types.encoding.HLA1516eInteger32BE;

import java.util.Map;

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

    public static void sendInteraction(
            RTIambassador rtiamb,
            HLAfloat64TimeFactory timeFactory,
            double time,
            byte[] tag,
            InteractionClassHandle interactionClassHandle,
            Map<String, byte[]> parameters
    ) throws RTIexception {
        ParameterHandleValueMap parameterHandleValueMap = rtiamb.getParameterHandleValueMapFactory().create(parameters.size());

        for (String parameter : parameters.keySet()) {
            ParameterHandle parameterHandle = rtiamb.getParameterHandle(interactionClassHandle, parameter);
            parameterHandleValueMap.put(parameterHandle, parameters.get(parameter));
        }

        HLAfloat64Time hlaTime = timeFactory.makeTime(time);
        rtiamb.sendInteraction(interactionClassHandle, parameterHandleValueMap, tag, hlaTime);
    }
}
