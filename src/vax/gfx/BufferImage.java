package vax.gfx;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 POJO data wrapper

 @author toor
 */
public class BufferImage {
    public int width, height;
    public int componentCount, bitDepth;
    public ByteBuffer buffer;

    public BufferImage ( int width, int height, int componentCount, int bitDepth ) {
        this.width = width;
        this.height = height;
        this.componentCount = componentCount;
        this.bitDepth = bitDepth;
    }

    public BufferImage ( int width, int height, int componentCount, int bitDepth, ByteBuffer buffer ) {
        this( width, height, componentCount, bitDepth );
        this.buffer = buffer;
    }

    public BufferImage ( int width, int height, int componentCount, int bitDepth, boolean allocateDirect ) {
        this(width, height, componentCount, bitDepth );
        buffer = allocateDirect ? BufferUtils.createByteBuffer( _calcNeededBufferSize() )
                : ByteBuffer.wrap( new byte[_calcNeededBufferSize()] );
    }

    public BufferImage ( int width, int height, int componentCount ) {
        this( width, height, componentCount, Byte.SIZE, true );
    }
/*
    public BufferImage ( Vector2i imageSize, int componentCount ) {
        this( imageSize.getX(), imageSize.getY(), componentCount );
    }
*/

    public int calcNeededBufferSize () {
        return _calcNeededBufferSize();
    }

    private int _calcNeededBufferSize () {
        if ( bitDepth != Byte.SIZE ) {
            throw new UnsupportedOperationException();
        }
        return width * height * componentCount;
    }

    public void saveTo ( String formatName, File file ) throws IOException {
        PNG.Encoder.write( this, file );
    }
}
