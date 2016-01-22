package vax.gfx;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import static vax.gfx.Main.packRGB;

/**

 @author toor
 */
public class ImageAnalysis {

    public static BufferedImage toBufferedImage ( BufferImage bi, BufferedImage jbi, int[] tempBuffer ) {
        ByteBuffer bb = bi.buffer;
        bb.rewind();
        for( int i = 0, max = tempBuffer.length; i < max; i++ ) {
            byte r = bb.get(), g = bb.get(), b = bb.get();
            tempBuffer[i] = packRGB( r, g, b );
        }
        jbi.setRGB( 0, 0, bi.width, bi.height, tempBuffer, 0, bi.width );
        return jbi;
    }

    public static BufferedImage toBufferedImageMask ( BufferImage bi, int[] output, BufferedImage jbi, int mask ) {
        ByteBuffer bb = bi.buffer;
        bb.rewind();
        for( int i = 0, max = output.length; i < max; i++ ) {
            byte r = (byte) ( bb.get() & mask ), g = (byte) ( bb.get() & mask ), b = (byte) ( bb.get() & mask );
            output[i] = packRGB( r, g, b );
        }
        jbi.setRGB( 0, 0, bi.width, bi.height, output, 0, bi.width );
        return jbi;
    }

    public static BufferedImage maskByteBuffer ( BufferImage bi, int[] output, BufferedImage jbi, int mask ) {
        return maskByteBuffer( bi, output, jbi, (a, b, c) -> a & mask, (a, b, c) -> b & mask, (a, b, c) -> c & mask );
    }

    public interface IntTripletFunction {
        int accept ( int a, int b, int c );
    }

    public static BufferedImage maskByteBuffer ( BufferImage bi, int[] output, BufferedImage jbi,
            IntTripletFunction tripR, IntTripletFunction tripG, IntTripletFunction tripB ) {
        maskByteBuffer( bi, output, tripR, tripG, tripB );
        jbi.setRGB( 0, 0, bi.width, bi.height, output, 0, bi.width );
        return jbi;
    }

    public static void maskByteBuffer ( BufferImage bi, int[] output,
            IntTripletFunction tripR, IntTripletFunction tripG, IntTripletFunction tripB ) {
        ByteBuffer bb = bi.buffer;
        bb.rewind();
        for( int i = 0, max = bb.remaining() / 3; i < max; i++ ) {
            int pos = bb.position();

            byte r = bb.get(),
                    g = bb.get(),
                    b = bb.get();
            int iR = Byte.toUnsignedInt( r ), iG = Byte.toUnsignedInt( g ), iB = Byte.toUnsignedInt( b );
            r = (byte) tripR.accept( iR, iG, iB );
            g = (byte) tripG.accept( iR, iG, iB );
            b = (byte) tripB.accept( iR, iG, iB );
            bb.position( pos );
            bb.put( r );
            bb.put( g );
            bb.put( b );
            if ( output != null ) {
                output[i] = packRGB( r, g, b );
            }
        }
    }

    public static class Histogram {
        /**
         output length is 3 dim, so == value range^3

         @param bb
         @param output
         @param scanLength
         @param rawCountOutput
         @return
         */
        public static double[] calcHistogram3 ( ByteBuffer bb, double[] output, int scanLength, int[] rawCountOutput ) {
            bb.rewind();
            int total = bb.remaining();
            HistoTest.fill( rawCountOutput, 0 );
            int scanLengthSq = scanLength * scanLength;

            for( int i = 0; i < total; i += 3 ) {
                int a = Byte.toUnsignedInt( bb.get() ),
                        b = Byte.toUnsignedInt( bb.get() ),
                        c = Byte.toUnsignedInt( bb.get() ); // RGB actually
                rawCountOutput[a + scanLength * b + scanLengthSq * c]++;
            }
            double invTotal = 1.0f / total;
            for( int i = rawCountOutput.length - 1; i > 0; i-- ) {
                output[i] = rawCountOutput[i] * invTotal;
            }
            return output;
        }

        /**
         output length is 3 dim, so == value range^3

         @param bb
         @param output
         @param scanLength
         @param rawCountOutput
         @return
         */
        public static double[] calcHistogramHSB3 ( ByteBuffer bb, double[] output, int scanLength, int[] rawCountOutput ) {
            bb.rewind();
            int total = bb.remaining();
            HistoTest.fill( rawCountOutput, 0 );
            int scanLengthSq = scanLength * scanLength;
            float[] hsb = new float[3];

            for( int i = 0; i < total; i += 3 ) {
                int a = Byte.toUnsignedInt( bb.get() ),
                        b = Byte.toUnsignedInt( bb.get() ),
                        c = Byte.toUnsignedInt( bb.get() ); // RGB actually
                Color.RGBtoHSB( a, b, c, hsb );
                rawCountOutput[(int) ( hsb[0] * 255 ) + (int) ( scanLength * hsb[1] * 255 ) + (int) ( scanLengthSq * hsb[2] * 255 )]++;
            }
            double invTotal = 1.0f / total;
            for( int i = rawCountOutput.length - 1; i > 0; i-- ) {
                output[i] = rawCountOutput[i] * invTotal;
            }
            return output;
        }

        /**
         output length is 1 dim only, == value range

         @param bb
         @param output
         @param scanLength
         @param rawCountOutput
         @return
         */
        public static double[] calcHistogramHue ( ByteBuffer bb, double[] output, int scanLength, int[] rawCountOutput ) {
            bb.rewind();
            int total = bb.remaining();
            HistoTest.fill( rawCountOutput, 0 );
            float[] hsb = new float[3];

            for( int i = 0; i < total; i += 3 ) {
                int a = Byte.toUnsignedInt( bb.get() ),
                        b = Byte.toUnsignedInt( bb.get() ),
                        c = Byte.toUnsignedInt( bb.get() ); // RGB actually
                Color.RGBtoHSB( a, b, c, hsb );
                output[(int) ( hsb[0] * scanLength )]++;
            }
            double invTotal = 1.0f / total;
            for( int i = rawCountOutput.length - 1; i > 0; i-- ) {
                output[i] = rawCountOutput[i] * invTotal;
            }
            return output;
        }

        private Histogram () {
            throw new UnsupportedOperationException();
        }
    }

    public static class Correlogram {
        public static double[] generate ( int maxFeatureValue, BufferImage bi ) {
            return generate( maxFeatureValue, new int[]{ 1, 3, 5, 7 }, bi );
        }

        private static int get ( ByteBuffer src, int x, int y, int width ) {
            return Byte.toUnsignedInt( src.get( x + y * width ) );
        }

        public static double[] generate ( int maxFeatureValue, int[] distanceSet, BufferImage bi ) {
            int[] histogram = new int[maxFeatureValue];
            int N_DIST = distanceSet.length;
            double[] correlogram = new double[maxFeatureValue * N_DIST];

            int w = bi.width;
            int h = bi.height;

            ByteBuffer bb = bi.buffer;
            bb.rewind();

            for( int i = bb.remaining(); i > 0; i-- ) {
                histogram[Byte.toUnsignedInt( bb.get() )]++;
            }
            bb.rewind();

            for( int di = 0; di < N_DIST; ++di ) {
                int d = distanceSet[di];
                for( int x = 0; x < w; ++x ) {
                    for( int y = 0; y < h; ++y ) {
                        int c = get( bb, x, y, w );
                        for( int dx = -d; dx <= d; dx++ ) {
                            int X = x + dx, Y = y - d;
                            if ( 0 <= X && X < w && 0 <= Y && Y < h && get( bb, X, Y, w ) == c ) {
                                correlogram[c + di * N_DIST]++;
                            }
                            Y = y + d;
                            if ( 0 <= X && X < w && 0 <= Y && Y < h && get( bb, X, Y, w ) == c ) {
                                correlogram[c + di * N_DIST]++;
                            }
                        }
                        for( int dy = -d + 1; dy <= d - 1; dy++ ) {
                            int X = x - d, Y = y + dy;
                            if ( 0 <= X && X < w && 0 <= Y && Y < h && get( bb, X, Y, w ) == c ) {
                                correlogram[c + di * N_DIST]++;
                            }
                            X = x + d;
                            if ( 0 <= X && X < w && 0 <= Y && Y < h && get( bb, X, Y, w ) == c ) {
                                correlogram[c + di * N_DIST]++;
                            }
                        }
                    }
                }
                double d8 = 8.0f * d;
                for( int c = 0; c < maxFeatureValue; ++c ) {
                    if ( histogram[c] > 0 ) {
                        correlogram[c + di * N_DIST] = Math.floor( 16 * ( correlogram[c + di * N_DIST] / ( histogram[c] * d8 ) ) );
                    }
                }
            }
            return correlogram;
        }

        private Correlogram () {
            throw new UnsupportedOperationException();
        }
    }

    public static class Distance {
        public static double calcManhattanDistance ( double[] v1, double[] v2 ) {
            int max = v1.length;
            if ( max != v2.length ) {
                throw new UnsupportedOperationException( "length mismatch: " + v1.length + " != " + v2.length );
            }
            double dist = 0;
            for( int i = 0; i < max; i++ ) {
                dist += Math.abs( v1[i] - v2[i] );
            }
            return dist;
        }

        public static double calcEuclideanDistance ( double[] v1, double[] v2 ) {
            int max = v1.length;
            if ( max != v2.length ) {
                throw new UnsupportedOperationException( "length mismatch: " + v1.length + " != " + v2.length );
            }
            double dist = 0, f;
            for( int i = 0; i < max; i++ ) {
                f = v1[i] - v2[i];
                f *= f;
                dist += f;
            }
            return Math.sqrt( dist );
        }

        // note: not a "real" distance
        public static double calcCosineDistance ( double[] v1, double[] v2 ) {
            int max = v1.length;
            if ( max != v2.length ) {
                throw new UnsupportedOperationException( "length mismatch: " + v1.length + " != " + v2.length );
            }
            double dist = 0, v1len = 0, v2len = 0;
            for( int i = 0; i < max; i++ ) {
                dist += v1[i] * v2[i];
                v1len += v1[i] * v1[i];
                v2len += v2[i] * v2[i];
            }
            return 1.0f - dist / Math.sqrt( v1len * v2len );
        }

        public static double calcIntersection ( double[] v1, double[] v2 ) {
            int max = v1.length;
            if ( max != v2.length ) {
                throw new UnsupportedOperationException( "length mismatch: " + v1.length + " != " + v2.length );
            }
            double dist = 0;
            for( int i = 0; i < max; i++ ) {
                dist += Math.min( v1[i], v2[i] );
            }
            return 1.0f - dist / max;
        }

        public static double calcNormalizedCrossCorrelation ( double[] v1, double[] v2 ) {
            int max = v1.length;
            if ( max != v2.length ) {
                throw new UnsupportedOperationException( "length mismatch: " + v1.length + " != " + v2.length );
            }
            double dist = 0, denom = 0;
            for( int i = 0; i < max; i++ ) {
                dist += v1[i] * v2[i];
                denom += v1[i] * v1[i];
            }
            return dist / denom;
        }

        // note 1: vectors have to be normalized to be able to be treated as probability distribution
        public static double calcKLDistance ( double[] v1, double[] v2 ) {
            int max = v1.length;
            if ( max != v2.length ) {
                throw new UnsupportedOperationException( "length mismatch: " + v1.length + " != " + v2.length );
            }
            double dist = 0;
            for( int i = 0; i < max; i++ ) {
                if ( v1[i] != 0 && v2[i] != 0 ) {
                    dist += v1[i] * log2( v1[i] / v2[i] );
                }
            }
            return dist;
        }

        // note: vectors have to be normalized to be able to be treated as probability distribution
        public static double calcJeffreyDistance ( double[] v1, double[] v2 ) {
            int max = v1.length;
            if ( max != v2.length ) {
                throw new UnsupportedOperationException( "length mismatch: " + v1.length + " != " + v2.length );
            }
            double dist = 0, m;
            for( int i = 0; i < max; i++ ) {
                m = v1[i] + v2[i]; // >= 0 by def
                if ( v1[i] != 0 ) {
                    dist += v1[i] * log2( v1[i] / m );
                }
                if ( v2[i] != 0 ) {
                    dist += v2[i] * log2( v2[i] / m );
                }
            }
            return dist;
        }

        public static final double LN_2 = Math.log( 2 );

        public static double log2 ( double f ) {
            return Math.log( f ) / LN_2;
        }

        private Distance () {
            throw new UnsupportedOperationException();
        }
    }

    private ImageAnalysis () {
        throw new UnsupportedOperationException();
    }
}
