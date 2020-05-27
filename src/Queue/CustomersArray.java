package Queue;

import hla.rti1516e.encoding.*;

import java.util.Iterator;
import java.util.List;

public class CustomersArray implements HLAvariableArray {
    private List<Integer> customerIds;

    public CustomersArray(List<Integer> customerIds) {
        this.customerIds = customerIds;
    }

    @Override
    public void addElement(DataElement dataElement) {

    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public DataElement get(int i) {
        return null;
    }

    @Override
    public Iterator iterator() {
        return null;
    }

    @Override
    public void resize(int i) {

    }

    @Override
    public int getOctetBoundary() {
        return 0;
    }

    @Override
    public void encode(ByteWrapper byteWrapper) throws EncoderException {

    }

    @Override
    public int getEncodedLength() {
        return 0;
    }

    @Override
    public byte[] toByteArray() throws EncoderException {
        return new byte[0];
    }

    @Override
    public void decode(ByteWrapper byteWrapper) throws DecoderException {

    }

    @Override
    public void decode(byte[] bytes) throws DecoderException {

    }
}
