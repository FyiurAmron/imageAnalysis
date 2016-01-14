package vax.gfx;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.*;

/**

 @author toor
 */
public class PNG {
    private static final byte[] SIGNATURE = { (byte) 137, 80, 78, 71, 13, 10, 26, 10 };

    private static final int //
            IHDR = 0x49484452,
            IDAT = 0x49444154,
            IEND = 0x49454E44,
            PLTE = 0x504C5445,
            tRNS = 0x74524E53;

    private static final byte //
            COLOR_GREYSCALE = 0,
            COLOR_RGB8 = 2,
            COLOR_INDEXED = 3,
            COLOR_GREYALPHA = 4,
            COLOR_RGBA8 = 6;

    private static final byte //
            COMPRESSION_DEFLATE = 0, // only one that's currently supported
            ADAPTIVE_FILTER = 0, // only one that's currently supported
            INTERLACE_NONE = 0,
            INTERLACE_ADAM7 = 1;
    private static final byte PAETH = 4;

    public enum Format {
        ALPHA( 1, true ),
        LUMINANCE( 1, false ),
        LUMINANCE_ALPHA( 2, true ),
        RGB( 3, false ),
        RGBA( 4, true ),
        BGRA( 4, true ),
        ABGR( 4, true );

        final int numComponents;
        final boolean hasAlpha;

        private Format ( int numComponents, boolean hasAlpha ) {
            this.numComponents = numComponents;
            this.hasAlpha = hasAlpha;
        }

        public int getNumComponents () {
            return numComponents;
        }

        public boolean isHasAlpha () {
            return hasAlpha;
        }
    }

    /**
     Simple PNG writer with the ability to save buffered pixmap data.

     @author Matthias Mann
     @author vaxquis
     */
    static public class Encoder {
        public static void write ( BufferImage bi, OutputStream os ) throws IOException {
            write( bi.width, bi.height, bi.componentCount, bi.bitDepth, bi.buffer.asReadOnlyBuffer(), os );
        }

        public static void write ( int height, int width, int componentCount, int bitDepth, ByteBuffer byteBuffer, OutputStream os )
                throws IOException {
            DataOutputStream dos = new DataOutputStream( os );
            dos.write( SIGNATURE );

            Chunk cIHDR = new Chunk( IHDR );
            cIHDR.writeInt( width );
            cIHDR.writeInt( height );
            cIHDR.writeByte( bitDepth );
            cIHDR.writeByte( ColorType.fromComponentCount( componentCount ).getPngConstant() );
            cIHDR.writeByte( COMPRESSION_DEFLATE );
            cIHDR.writeByte( ADAPTIVE_FILTER );
            cIHDR.writeByte( INTERLACE_NONE );
            cIHDR.writeTo( dos );

            Chunk cIDAT = new Chunk( IDAT );
            DeflaterOutputStream dfos = new DeflaterOutputStream( cIDAT, new Deflater( Deflater.BEST_COMPRESSION ) );

            int lineLen = width * componentCount;
            byte[] //
                    lineOut = new byte[lineLen],
                    curLine = new byte[lineLen],
                    prevLine = new byte[lineLen];

            int pos = ( height - 1 ) * lineLen;
            for( int line = 0; line < height; line++, pos -= lineLen ) {
                byteBuffer.position( pos );
                byteBuffer.get( curLine );

                lineOut[0] = (byte) ( curLine[0] - prevLine[0] );
                lineOut[1] = (byte) ( curLine[1] - prevLine[1] );
                lineOut[2] = (byte) ( curLine[2] - prevLine[2] );

                for( int x = 3; x < lineLen; x++ ) {
                    int px = x - 3;
                    int a = curLine[px] & 255;
                    int b = prevLine[x] & 255;
                    int c = prevLine[px] & 255;

                    int pa = Math.abs( b - c );
                    int pb = Math.abs( a - c );
                    int pc = Math.abs( a + b - 2 * c );
                    if ( pa <= pb && pa <= pc ) {
                        c = a;
                    } else if ( pb <= pc ) {
                        c = b;
                    }
                    lineOut[x] = (byte) ( curLine[x] - c );
                }

                dfos.write( PAETH );
                dfos.write( lineOut );

                byte[] temp = curLine; // swap the line buffers
                curLine = prevLine;
                prevLine = temp;
            }

            dfos.finish();
            cIDAT.writeTo( dos );

            Chunk cIEND = new Chunk( IEND );
            cIEND.writeTo( dos );

            dos.flush();
        }

        /**
         @param file it will be overwritten
         @param bi
         @throws java.io.IOException
         */
        public static void write ( BufferImage bi, File file ) throws IOException {
            try ( FileOutputStream fos = new FileOutputStream( file ) ) {
                write( bi, fos );
            }
        }

        /**
         Creates a job for use with {@link java.util.concurrent.ExecutorService}

         @param file it will be overwritten
         @param bi
         @return
         */
        public Runnable createJob ( BufferImage bi, File file ) {
            return () -> {
                try {
                    write( bi, file );
                } catch (IOException ex) {
                    throw new RuntimeException( ex );
                }
            };
        }

        private static class Chunk extends DataOutputStream {
            final CRC32 crc;
            final ByteArrayOutputStream baos;

            private Chunk ( int chunkType ) throws IOException {
                this( chunkType, new ByteArrayOutputStream(), new CRC32() );
            }

            private Chunk ( int chunkType, ByteArrayOutputStream baos,
                    CRC32 crc ) throws IOException {
                super( new CheckedOutputStream( baos, crc ) );
                this.crc = crc;
                this.baos = baos;

                writeInt( chunkType );
            }

            public void writeTo ( DataOutputStream out ) throws IOException {
                flush();
                out.writeInt( baos.size() - 4 );
                baos.writeTo( out );
                out.writeInt( (int) crc.getValue() );
            }
        }
    }

    /**
     A PNG Decoder. The slick PNG decoder is based on this class

     @author Matthias Mann
     @author vaquis
     */
    static public class Decoder {
        /*
         Copyright (c) 2008-2010, Matthias Mann

         All rights reserved.

         Redistribution and use in source and binary forms, with or without
         modification, are permitted provided that the following conditions are met:

         Redistributions of source code must retain the above copyright notice,
         this list of conditions and the following disclaimer.
         Redistributions in binary form must reproduce the above copyright
         notice, this list of conditions and the following disclaimer in the
         documentation and/or other materials provided with the distribution.
         Neither the name of Matthias Mann nor the names of its contributors may
         be used to endorse or promote products derived from this software
         without specific prior written permission.
         Redistributions in source or binary form must keep the original package
         and class name.

         THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
         "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
         LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
         A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
         CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
         EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
         PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
         PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
         LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
         NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
         SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
         */
// package de.matthiasmann.twl.utils;

        private final InputStream input;
        private final CRC32 crc;
        private final byte[] buffer;

        private int chunkLength, chunkType, chunkRemaining;

        private int width, height;
        private int bitdepth, colorType, bytesPerPixel;
        private byte[] palette, paletteA, transPixel;

        public Decoder ( InputStream input ) throws IOException {
            this.input = input;
            this.crc = new CRC32();
            this.buffer = new byte[4096];

            readFully( buffer, 0, SIGNATURE.length );
            if ( !checkSignature( buffer ) ) {
                throw new IOException( "Not a valid PNG file" );
            }

            openChunk( IHDR );
            readIHDR();
            closeChunk();

            while( true ) {
                openChunk();
                if ( chunkType == IDAT ) {
                    break;
                }
                switch ( chunkType ) {
                    case PLTE:
                        readPLTE();
                        break;
                    case tRNS:
                        readtRNS();
                        break;
                }
                closeChunk();
            }

            if ( colorType == COLOR_INDEXED && palette == null ) {
                throw new IOException( "Missing PLTE chunk" );
            }
        }

        public int getHeight () {
            return height;
        }

        public int getWidth () {
            return width;
        }

        public int getBitdepth () {
            return bitdepth;
        }

        public int getBytesPerPixel () {
            return bytesPerPixel;
        }

        public int getColorType () {
            return colorType;
        }

        /**
         Checks if the image has a real alpha channel.
         This method does not check for the presence of a tRNS chunk.

         @return true if the image has an alpha channel
         @see #hasAlpha()
         */
        public boolean hasAlphaChannel () {
            return colorType == COLOR_RGBA8 || colorType == COLOR_GREYALPHA;
        }

        /**
         Checks if the image has transparency information either from
         an alpha channel or from a tRNS chunk.

         @return true if the image has transparency
         @see #hasAlphaChannel()
         @see #overwriteTRNS(byte, byte, byte)
         */
        public boolean hasAlpha () {
            return hasAlphaChannel()
                    || paletteA != null || transPixel != null;
        }

        public boolean isRGB () {
            return colorType == COLOR_RGBA8
                    || colorType == COLOR_RGB8
                    || colorType == COLOR_INDEXED;
        }

        /**
         Overwrites the tRNS chunk entry to make a selected color transparent.
         <p>
         This can only be invoked when the image has no alpha channel.</p>
         <p>
         Calling this method causes {@link #hasAlpha()} to return true.</p>

         @param r the red component of the color to make transparent
         @param g the green component of the color to make transparent
         @param b the blue component of the color to make transparent
         @throws UnsupportedOperationException if the tRNS chunk data can't be set
         @see #hasAlphaChannel()
         */
        public void overwriteTRNS ( byte r, byte g, byte b ) {
            if ( hasAlphaChannel() ) {
                throw new UnsupportedOperationException( "image has an alpha channel" );
            }
            byte[] pal = this.palette;
            if ( pal == null ) {
                transPixel = new byte[]{ 0, r, 0, g, 0, b };
            } else {
                paletteA = new byte[pal.length / 3];
                for( int i = 0, j = 0; i < pal.length; i += 3, j++ ) {
                    if ( pal[i] != r || pal[i + 1] != g || pal[i + 2] != b ) {
                        paletteA[j] = (byte) 0xFF;
                    }
                }
            }
        }

        /**
         Computes the implemented format conversion for the desired format.

         @param fmt the desired format
         @return format which best matches the desired format
         @throws UnsupportedOperationException if this PNG file can't be decoded
         */
        public Format decideTextureFormat ( Format fmt ) {
            switch ( colorType ) {
                case COLOR_RGB8:
                    switch ( fmt ) {
                        case ABGR:
                        case RGBA:
                        case BGRA:
                        case RGB:
                            return fmt;
                        default:
                            return Format.RGB;
                    }
                case COLOR_RGBA8:
                    switch ( fmt ) {
                        case ABGR:
                        case RGBA:
                        case BGRA:
                        case RGB:
                            return fmt;
                        default:
                            return Format.RGBA;
                    }
                case COLOR_GREYSCALE:
                    switch ( fmt ) {
                        case LUMINANCE:
                        case ALPHA:
                            return fmt;
                        default:
                            return Format.LUMINANCE;
                    }
                case COLOR_GREYALPHA:
                    return Format.LUMINANCE_ALPHA;
                case COLOR_INDEXED:
                    switch ( fmt ) {
                        case ABGR:
                        case RGBA:
                        case BGRA:
                            return fmt;
                        default:
                            return Format.RGBA;
                    }
                default:
                    throw new UnsupportedOperationException( "Not yet implemented" );
            }
        }

        /**
         Decodes the image into the specified buffer. The first line is placed at
         the current position. After decode the buffer position is at the end of
         the last line. The stride is calculated automagically.

         @param buffer the buffer
         @param format the target format into which the image should be decoded.
         @throws IOException if a read or data error occurred
         @throws IllegalArgumentException if the start position of a line falls outside the buffer
         @throws UnsupportedOperationException if the image can't be decoded into the desired format
         */
        public void decode ( ByteBuffer buffer, Format format ) throws IOException {
            decode( buffer, width * bytesPerPixel, format );
        }

        /**
         Decodes the image into the specified buffer. The first line is placed at
         the current position. After decode the buffer position is at the end of
         the last line.

         @param buffer the buffer
         @param stride the stride in bytes from start of a line to start of the next line, can be negative.
         @param format the target format into which the image should be decoded.
         @throws IOException if a read or data error occurred
         @throws IllegalArgumentException if the start position of a line falls outside the buffer
         @throws UnsupportedOperationException if the image can't be decoded into the desired format
         */
        public void decode ( ByteBuffer buffer, int stride, Format format ) throws IOException {
            final int offset = buffer.position();
            final int lineSize = ( ( width * bitdepth + 7 ) / 8 ) * bytesPerPixel;
            byte[] curLine = new byte[lineSize + 1];
            byte[] prevLine = new byte[lineSize + 1];
            byte[] palLine = ( bitdepth < 8 ) ? new byte[width + 1] : null;

            final Inflater inflater = new Inflater();
            try {
                for( int y = 0; y < height; y++ ) {
                    readChunkUnzip( inflater, curLine, 0, curLine.length );
                    unfilter( curLine, prevLine );

                    buffer.position( offset + y * stride );

                    switch ( colorType ) {
                        case COLOR_RGB8:
                            switch ( format ) {
                                case ABGR:
                                    copyRGBtoABGR( buffer, curLine );
                                    break;
                                case RGBA:
                                    copyRGBtoRGBA( buffer, curLine );
                                    break;
                                case BGRA:
                                    copyRGBtoBGRA( buffer, curLine );
                                    break;
                                case RGB:
                                    copy( buffer, curLine );
                                    break;
                                default:
                                    throw new UnsupportedOperationException( "Unsupported format for this image" );
                            }
                            break;
                        case COLOR_RGBA8:
                            switch ( format ) {
                                case ABGR:
                                    copyRGBAtoABGR( buffer, curLine );
                                    break;
                                case RGBA:
                                    copy( buffer, curLine );
                                    break;
                                case BGRA:
                                    copyRGBAtoBGRA( buffer, curLine );
                                    break;
                                case RGB:
                                    copyRGBAtoRGB( buffer, curLine );
                                    break;
                                default:
                                    throw new UnsupportedOperationException( "Unsupported format for this image" );
                            }
                            break;
                        case COLOR_GREYSCALE:
                            switch ( format ) {
                                case LUMINANCE:
                                case ALPHA:
                                    copy( buffer, curLine );
                                    break;
                                default:
                                    throw new UnsupportedOperationException( "Unsupported format for this image" );
                            }
                            break;
                        case COLOR_GREYALPHA:
                            switch ( format ) {
                                case LUMINANCE_ALPHA:
                                    copy( buffer, curLine );
                                    break;
                                default:
                                    throw new UnsupportedOperationException( "Unsupported format for this image" );
                            }
                            break;
                        case COLOR_INDEXED:
                            switch ( bitdepth ) {
                                case 8:
                                    palLine = curLine;
                                    break;
                                case 4:
                                    expand4( curLine, palLine );
                                    break;
                                case 2:
                                    expand2( curLine, palLine );
                                    break;
                                case 1:
                                    expand1( curLine, palLine );
                                    break;
                                default:
                                    throw new UnsupportedOperationException( "Unsupported bitdepth for this image" );
                            }
                            switch ( format ) {
                                case ABGR:
                                    copyPALtoABGR( buffer, palLine );
                                    break;
                                case RGBA:
                                    copyPALtoRGBA( buffer, palLine );
                                    break;
                                case BGRA:
                                    copyPALtoBGRA( buffer, palLine );
                                    break;
                                default:
                                    throw new UnsupportedOperationException( "Unsupported format for this image" );
                            }
                            break;
                        default:
                            throw new UnsupportedOperationException( "Not yet implemented" );
                    }

                    byte[] tmp = curLine;
                    curLine = prevLine;
                    prevLine = tmp;
                }
            } finally {
                inflater.end();
            }
        }

        /**
         Decodes the image into the specified buffer. The last line is placed at
         the current position. After decode the buffer position is at the end of
         the first line.

         @param buffer the buffer
         @param stride the stride in bytes from start of a line to start of the next line, must be positive.
         @param fmt the target format into which the image should be decoded.
         @throws IOException if a read or data error occurred
         @throws IllegalArgumentException if the start position of a line falls outside the buffer
         @throws UnsupportedOperationException if the image can't be decoded into the desired format
         */
        public void decodeFlipped ( ByteBuffer buffer, int stride, Format fmt ) throws IOException {
            if ( stride <= 0 ) {
                throw new IllegalArgumentException( "stride" );
            }
            int pos = buffer.position();
            int posDelta = ( height - 1 ) * stride;
            buffer.position( pos + posDelta );
            decode( buffer, -stride, fmt );
            buffer.position( buffer.position() + posDelta );
        }

        private void copy ( ByteBuffer buffer, byte[] curLine ) {
            buffer.put( curLine, 1, curLine.length - 1 );
        }

        private void copyRGBtoABGR ( ByteBuffer buffer, byte[] curLine ) {
            if ( transPixel != null ) {
                byte tr = transPixel[1];
                byte tg = transPixel[3];
                byte tb = transPixel[5];
                for( int i = 1, n = curLine.length; i < n; i += 3 ) {
                    byte r = curLine[i];
                    byte g = curLine[i + 1];
                    byte b = curLine[i + 2];
                    byte a = ( r == tr && g == tg && b == tb ) ? 0 : (byte) 0xFF;
                    buffer.put( a ).put( b ).put( g ).put( r );
                }
            } else {
                for( int i = 1, n = curLine.length; i < n; i += 3 ) {
                    buffer.put( (byte) 0xFF ).put( curLine[i + 2] ).put( curLine[i + 1] ).put( curLine[i] );
                }
            }
        }

        private void copyRGBtoRGBA ( ByteBuffer buffer, byte[] curLine ) {
            if ( transPixel != null ) {
                byte tr = transPixel[1];
                byte tg = transPixel[3];
                byte tb = transPixel[5];
                for( int i = 1, n = curLine.length; i < n; i += 3 ) {
                    byte r = curLine[i];
                    byte g = curLine[i + 1];
                    byte b = curLine[i + 2];
                    byte a = ( r == tr && g == tg && b == tb ) ? 0 : (byte) 0xFF;
                    buffer.put( r ).put( g ).put( b ).put( a );
                }
            } else {
                for( int i = 1, n = curLine.length; i < n; i += 3 ) {
                    buffer.put( curLine[i] ).put( curLine[i + 1] ).put( curLine[i + 2] ).put( (byte) 0xFF );
                }
            }
        }

        private void copyRGBtoBGRA ( ByteBuffer buffer, byte[] curLine ) {
            if ( transPixel != null ) {
                byte tr = transPixel[1];
                byte tg = transPixel[3];
                byte tb = transPixel[5];
                for( int i = 1, n = curLine.length; i < n; i += 3 ) {
                    byte r = curLine[i];
                    byte g = curLine[i + 1];
                    byte b = curLine[i + 2];
                    byte a = ( r == tr && g == tg && b == tb ) ? 0 : (byte) 0xFF;
                    buffer.put( b ).put( g ).put( r ).put( a );
                }
            } else {
                for( int i = 1, n = curLine.length; i < n; i += 3 ) {
                    buffer.put( curLine[i + 2] ).put( curLine[i + 1] ).put( curLine[i] ).put( (byte) 0xFF );
                }
            }
        }

        private void copyRGBAtoABGR ( ByteBuffer buffer, byte[] curLine ) {
            for( int i = 1, n = curLine.length; i < n; i += 4 ) {
                buffer.put( curLine[i + 3] ).put( curLine[i + 2] ).put( curLine[i + 1] ).put( curLine[i] );
            }
        }

        private void copyRGBAtoBGRA ( ByteBuffer buffer, byte[] curLine ) {
            for( int i = 1, n = curLine.length; i < n; i += 4 ) {
                buffer.put( curLine[i + 2] ).put( curLine[i + 1] ).put( curLine[i] ).put( curLine[i + 3] );
            }
        }

        private void copyRGBAtoRGB ( ByteBuffer buffer, byte[] curLine ) {
            for( int i = 1, n = curLine.length; i < n; i += 4 ) {
                buffer.put( curLine[i] ).put( curLine[i + 1] ).put( curLine[i + 2] );
            }
        }

        private void copyPALtoABGR ( ByteBuffer buffer, byte[] curLine ) {
            if ( paletteA != null ) {
                for( int i = 1, n = curLine.length; i < n; i++ ) {
                    int idx = curLine[i] & 255;
                    buffer.put( paletteA[idx] )
                            .put( palette[idx * 3 + 2] ).
                            put( palette[idx * 3 + 1] )
                            .put( palette[idx * 3] );
                }
            } else {
                for( int i = 1, n = curLine.length; i < n; i++ ) {
                    int idx = curLine[i] & 255;
                    buffer.put( (byte) 0xFF )
                            .put( palette[idx * 3 + 2] )
                            .put( palette[idx * 3 + 1] )
                            .put( palette[idx * 3] );
                }
            }
        }

        private void copyPALtoRGBA ( ByteBuffer buffer, byte[] curLine ) {
            if ( paletteA != null ) {
                for( int i = 1, n = curLine.length; i < n; i++ ) {
                    int idx = curLine[i] & 255;
                    buffer.put( palette[idx * 3] )
                            .put( palette[idx * 3 + 1] )
                            .put( palette[idx * 3 + 2] )
                            .put( paletteA[idx] );
                }
            } else {
                for( int i = 1, n = curLine.length; i < n; i++ ) {
                    int idx = curLine[i] & 255;
                    buffer.put( palette[idx * 3] )
                            .put( palette[idx * 3 + 1] )
                            .put( palette[idx * 3 + 2] )
                            .put( (byte) 0xFF );
                }
            }
        }

        private void copyPALtoBGRA ( ByteBuffer buffer, byte[] curLine ) {
            if ( paletteA != null ) {
                for( int i = 1, n = curLine.length; i < n; i++ ) {
                    int idx = curLine[i] & 255;
                    buffer.put( palette[idx * 3 + 2] )
                            .put( palette[idx * 3 + 1] )
                            .put( palette[idx * 3] )
                            .put( paletteA[idx] );
                }
            } else {
                for( int i = 1, n = curLine.length; i < n; i++ ) {
                    int idx = curLine[i] & 255;
                    buffer.put( palette[idx * 3 + 2] )
                            .put( palette[idx * 3 + 1] )
                            .put( palette[idx * 3] )
                            .put( (byte) 0xFF );
                }
            }
        }

        @SuppressWarnings( "fallthrough" )
        private void expand4 ( byte[] src, byte[] dst ) {
            for( int i = 1, n = dst.length; i < n; i += 2 ) {
                int val = src[1 + ( i >> 1 )] & 255;
                switch ( n - i ) {
                    default:
                        dst[i + 1] = (byte) ( val & 15 );
                    case 1:
                        dst[i] = (byte) ( val >> 4 );
                }
            }
        }

        @SuppressWarnings( "fallthrough" )
        private void expand2 ( byte[] src, byte[] dst ) {
            for( int i = 1, n = dst.length; i < n; i += 4 ) {
                int val = src[1 + ( i >> 2 )] & 255;
                switch ( n - i ) {
                    default:
                        dst[i + 3] = (byte) ( ( val ) & 3 );
                    case 3:
                        dst[i + 2] = (byte) ( ( val >> 2 ) & 3 );
                    case 2:
                        dst[i + 1] = (byte) ( ( val >> 4 ) & 3 );
                    case 1:
                        dst[i] = (byte) ( ( val >> 6 ) );
                }
            }
        }

        @SuppressWarnings( "fallthrough" )
        private void expand1 ( byte[] src, byte[] dst ) {
            for( int i = 1, n = dst.length; i < n; i += 8 ) {
                int val = src[1 + ( i >> 3 )] & 255;
                switch ( n - i ) {
                    default:
                        dst[i + 7] = (byte) ( ( val ) & 1 );
                    case 7:
                        dst[i + 6] = (byte) ( ( val >> 1 ) & 1 );
                    case 6:
                        dst[i + 5] = (byte) ( ( val >> 2 ) & 1 );
                    case 5:
                        dst[i + 4] = (byte) ( ( val >> 3 ) & 1 );
                    case 4:
                        dst[i + 3] = (byte) ( ( val >> 4 ) & 1 );
                    case 3:
                        dst[i + 2] = (byte) ( ( val >> 5 ) & 1 );
                    case 2:
                        dst[i + 1] = (byte) ( ( val >> 6 ) & 1 );
                    case 1:
                        dst[i] = (byte) ( ( val >> 7 ) );
                }
            }
        }

        private void unfilter ( byte[] curLine, byte[] prevLine ) throws IOException {
            switch ( curLine[0] ) {
                case 0: // none
                    break;
                case 1:
                    unfilterSub( curLine );
                    break;
                case 2:
                    unfilterUp( curLine, prevLine );
                    break;
                case 3:
                    unfilterAverage( curLine, prevLine );
                    break;
                case 4:
                    unfilterPaeth( curLine, prevLine );
                    break;
                default:
                    throw new IOException( "invalide filter type in scanline: " + curLine[0] );
            }
        }

        private void unfilterSub ( byte[] curLine ) {
            final int bpp = this.bytesPerPixel;
            for( int i = bpp + 1, n = curLine.length; i < n; ++i ) {
                curLine[i] += curLine[i - bpp];
            }
        }

        private void unfilterUp ( byte[] curLine, byte[] prevLine ) {
            //final int bpp = this.bytesPerPixel;
            for( int i = 1, n = curLine.length; i < n; ++i ) {
                curLine[i] += prevLine[i];
            }
        }

        private void unfilterAverage ( byte[] curLine, byte[] prevLine ) {
            final int bpp = this.bytesPerPixel;

            int i;
            for( i = 1; i <= bpp; ++i ) {
                curLine[i] += (byte) ( ( prevLine[i] & 0xFF ) >>> 1 );
            }
            for( int n = curLine.length; i < n; ++i ) {
                curLine[i] += (byte) ( ( ( prevLine[i] & 0xFF ) + ( curLine[i - bpp] & 0xFF ) ) >>> 1 );
            }
        }

        private void unfilterPaeth ( byte[] curLine, byte[] prevLine ) {
            final int bpp = this.bytesPerPixel;

            int i;
            for( i = 1; i <= bpp; ++i ) {
                curLine[i] += prevLine[i];
            }
            for( int n = curLine.length; i < n; ++i ) {
                int a = curLine[i - bpp] & 255;
                int b = prevLine[i] & 255;
                int c = prevLine[i - bpp] & 255;
                int p = a + b - c;
                int pa = p - a;
                if ( pa < 0 ) {
                    pa = -pa;
                }
                int pb = p - b;
                if ( pb < 0 ) {
                    pb = -pb;
                }
                int pc = p - c;
                if ( pc < 0 ) {
                    pc = -pc;
                }
                if ( pa <= pb && pa <= pc ) {
                    c = a;
                } else if ( pb <= pc ) {
                    c = b;
                }
                curLine[i] += (byte) c;
            }
        }

        private void readIHDR () throws IOException {
            checkChunkLength( 13 );
            readChunk( buffer, 0, 13 );
            width = readInt( buffer, 0 );
            height = readInt( buffer, 4 );
            bitdepth = buffer[8] & 255;
            colorType = buffer[9] & 255;

            switch ( colorType ) {
                case COLOR_GREYSCALE:
                    if ( bitdepth != 8 ) {
                        throw new IOException( "Unsupported bit depth: " + bitdepth );
                    }
                    bytesPerPixel = 1;
                    break;
                case COLOR_GREYALPHA:
                    if ( bitdepth != 8 ) {
                        throw new IOException( "Unsupported bit depth: " + bitdepth );
                    }
                    bytesPerPixel = 2;
                    break;
                case COLOR_RGB8:
                    if ( bitdepth != 8 ) {
                        throw new IOException( "Unsupported bit depth: " + bitdepth );
                    }
                    bytesPerPixel = 3;
                    break;
                case COLOR_RGBA8:
                    if ( bitdepth != 8 ) {
                        throw new IOException( "Unsupported bit depth: " + bitdepth );
                    }
                    bytesPerPixel = 4;
                    break;
                case COLOR_INDEXED:
                    switch ( bitdepth ) {
                        case 8:
                        case 4:
                        case 2:
                        case 1:
                            bytesPerPixel = 1;
                            break;
                        default:
                            throw new IOException( "Unsupported bit depth: " + bitdepth );
                    }
                    break;
                default:
                    throw new IOException( "unsupported color format: " + colorType );
            }

            if ( buffer[10] != 0 ) {
                throw new IOException( "unsupported compression method" );
            }
            if ( buffer[11] != 0 ) {
                throw new IOException( "unsupported filtering method" );
            }
            if ( buffer[12] != 0 ) {
                throw new IOException( "unsupported interlace method" );
            }
        }

        private void readPLTE () throws IOException {
            int paletteEntries = chunkLength / 3;
            if ( paletteEntries < 1 || paletteEntries > 256 || ( chunkLength % 3 ) != 0 ) {
                throw new IOException( "PLTE chunk has wrong length" );
            }
            palette = new byte[paletteEntries * 3];
            readChunk( palette, 0, palette.length );
        }

        private void readtRNS () throws IOException {
            switch ( colorType ) {
                case COLOR_GREYSCALE:
                    checkChunkLength( 2 );
                    transPixel = new byte[2];
                    readChunk( transPixel, 0, 2 );
                    break;
                case COLOR_RGB8:
                    checkChunkLength( 6 );
                    transPixel = new byte[6];
                    readChunk( transPixel, 0, 6 );
                    break;
                case COLOR_INDEXED:
                    if ( palette == null ) {
                        throw new IOException( "tRNS chunk without PLTE chunk" );
                    }
                    paletteA = new byte[palette.length / 3];
                    Arrays.fill( paletteA, (byte) 0xFF );
                    readChunk( paletteA, 0, paletteA.length );
                    break;
                default:
                // just ignore it
            }
        }

        private void closeChunk () throws IOException {
            if ( chunkRemaining > 0 ) {
                // just skip the rest and the CRC
                skip( chunkRemaining + 4 );
            } else {
                readFully( buffer, 0, 4 );
                int expectedCrc = readInt( buffer, 0 );
                int computedCrc = (int) crc.getValue();
                if ( computedCrc != expectedCrc ) {
                    throw new IOException( "Invalid CRC" );
                }
            }
            chunkRemaining = 0;
            chunkLength = 0;
            chunkType = 0;
        }

        private void openChunk () throws IOException {
            readFully( buffer, 0, 8 );
            chunkLength = readInt( buffer, 0 );
            chunkType = readInt( buffer, 4 );
            chunkRemaining = chunkLength;
            crc.reset();
            crc.update( buffer, 4, 4 );   // only chunkType
        }

        private void openChunk ( int expected ) throws IOException {
            openChunk();
            if ( chunkType != expected ) {
                throw new IOException( "Expected chunk: " + Integer.toHexString( expected ) );
            }
        }

        private void checkChunkLength ( int expected ) throws IOException {
            if ( chunkLength != expected ) {
                throw new IOException( "Chunk has wrong size" );
            }
        }

        private int readChunk ( byte[] buffer, int offset, int length ) throws IOException {
            if ( length > chunkRemaining ) {
                length = chunkRemaining;
            }
            readFully( buffer, offset, length );
            crc.update( buffer, offset, length );
            chunkRemaining -= length;
            return length;
        }

        private void refillInflater ( Inflater inflater ) throws IOException {
            while( chunkRemaining == 0 ) {
                closeChunk();
                openChunk( IDAT );
            }
            int read = readChunk( buffer, 0, buffer.length );
            inflater.setInput( buffer, 0, read );
        }

        private void readChunkUnzip ( Inflater inflater, byte[] buffer, int offset, int length ) throws IOException {
            assert ( buffer != this.buffer );
            try {
                do {
                    int read = inflater.inflate( buffer, offset, length );
                    if ( read <= 0 ) {
                        if ( inflater.finished() ) {
                            throw new EOFException();
                        }
                        if ( inflater.needsInput() ) {
                            refillInflater( inflater );
                        } else {
                            throw new IOException( "Can't inflate " + length + " bytes" );
                        }
                    } else {
                        offset += read;
                        length -= read;
                    }
                } while( length > 0 );
            } catch (DataFormatException ex) {
                throw (IOException) ( new IOException( "inflate error" ).initCause( ex ) );
            }
        }

        private void readFully ( byte[] buffer, int offset, int length ) throws IOException {
            do {
                int read = input.read( buffer, offset, length );
                if ( read < 0 ) {
                    throw new EOFException();
                }
                offset += read;
                length -= read;
            } while( length > 0 );
        }

        private int readInt ( byte[] buffer, int offset ) {
            return ( ( buffer[offset] ) << 24 )
                    | ( ( buffer[offset + 1] & 255 ) << 16 )
                    | ( ( buffer[offset + 2] & 255 ) << 8 )
                    | ( ( buffer[offset + 3] & 255 ) );
        }

        private void skip ( long amount ) throws IOException {
            while( amount > 0 ) {
                long skipped = input.skip( amount );
                if ( skipped < 0 ) {
                    throw new EOFException();
                }
                amount -= skipped;
            }
        }

        private static boolean checkSignature ( byte[] buffer ) {
            for( int i = 0; i < SIGNATURE.length; i++ ) {
                if ( buffer[i] != SIGNATURE[i] ) {
                    return false;
                }
            }
            return true;
        }
    }

    private PNG () {
        throw new UnsupportedOperationException();
    }
}
