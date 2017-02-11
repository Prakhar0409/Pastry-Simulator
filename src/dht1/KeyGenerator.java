package dht1;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

public class KeyGenerator {
	public static byte[] generateRandomID(int length) {
        Random random = new Random();
        byte[] bytes = new byte[length];
        for(int i=0; i<bytes.length; i++) {
            bytes[i] = (byte) (random.nextInt() % 256);     //256 -> 2^8
        }
        return bytes;
    }

    public static long convertBytesToInt(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int result = buffer.getShort() & 0xffff; 		//only key size 16; bitmask for only +ve range        
        return (long)result;
    }

}
