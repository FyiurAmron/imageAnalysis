package vax.gfx;

import java.nio.ByteBuffer;

/**

 @author toor
 */
public class BufferUtils {
    public static ByteBuffer createByteBuffer ( int capacity ) {
        return ByteBuffer.allocate( capacity );
    }

    private BufferUtils () {
        throw new UnsupportedOperationException();
    }
}
