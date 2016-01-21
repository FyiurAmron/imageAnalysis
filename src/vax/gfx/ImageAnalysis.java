package vax.gfx;

import java.nio.ByteBuffer;

/**

 @author toor
 */
public class ImageAnalysis {
    public static class Histogram {
        public static float[] calcHistogram3 ( ByteBuffer bb, float[] output, int scanLength, int[] helper ) {
            bb.rewind();
            int total = bb.remaining();
            HistoTest.fill( helper, 0 );
            int scanLengthSq = scanLength * scanLength;

            for( int i = 0; i < total; i++ ) {
                byte a = bb.get(), b = bb.get(), c = bb.get(); // RGB actually
                helper[a + scanLength * b + scanLengthSq * c]++;
            }
            float invTotal = 1.0f / total;
            for( int i = helper.length - 1; i > 0; i-- ) {
                output[i] = helper[i] * invTotal;
            }
            return output;
        }
    }

    public static class Correlogram {

    }

    public static class Distance {
        public static float calcManhattanDistance ( float[] v1, float[] v2 ) {
            int max = v1.length;
            if ( max != v2.length ) {
                throw new UnsupportedOperationException( "length mismatch: " + v1.length + " != " + v2.length );
            }
            float dist = 0;
            for( int i = 0; i < max; i++ ) {
                dist += Math.abs( v1[i] - v2[i] );
            }
            return dist;
        }

        public static float calcEuclideanDistance ( float[] v1, float[] v2 ) {
            int max = v1.length;
            if ( max != v2.length ) {
                throw new UnsupportedOperationException( "length mismatch: " + v1.length + " != " + v2.length );
            }
            float dist = 0, f;
            for( int i = 0; i < max; i++ ) {
                f = v1[i] - v2[i];
                f *= f;
                dist += f;
            }
            return (float) Math.sqrt( dist );
        }

        // note: not a "real" distance
        public static float calcCosineDistance ( float[] v1, float[] v2 ) {
            int max = v1.length;
            if ( max != v2.length ) {
                throw new UnsupportedOperationException( "length mismatch: " + v1.length + " != " + v2.length );
            }
            float dist = 0, v1len = 0, v2len = 0;
            for( int i = 0; i < max; i++ ) {
                dist += v1[i] * v2[i];
                v1len += v1[i] * v1[i];
                v2len += v2[i] * v2[i];
            }
            return 1.0f - dist / (float) Math.sqrt( v1len * v2len );
        }

        public static float calcIntersection ( float[] v1, float[] v2 ) {
            int max = v1.length;
            if ( max != v2.length ) {
                throw new UnsupportedOperationException( "length mismatch: " + v1.length + " != " + v2.length );
            }
            float dist = 0;
            for( int i = 0; i < max; i++ ) {
                dist += Math.min( v1[i], v2[i] );
            }
            return 1.0f - dist / max;
        }

        public static float calcNormalizedCrossCorrelation ( float[] v1, float[] v2 ) {
            int max = v1.length;
            if ( max != v2.length ) {
                throw new UnsupportedOperationException( "length mismatch: " + v1.length + " != " + v2.length );
            }
            float dist = 0, denom = 0;
            for( int i = 0; i < max; i++ ) {
                dist += v1[i] * v2[i];
                denom += v1[i] * v1[i];
            }
            return dist / denom;
        }

        // note 1: vectors have to be normalized to be able to be treated as probability distribution
        public static float calcKLDistance ( float[] v1, float[] v2 ) {
            int max = v1.length;
            if ( max != v2.length ) {
                throw new UnsupportedOperationException( "length mismatch: " + v1.length + " != " + v2.length );
            }
            float dist = 0;
            for( int i = 0; i < max; i++ ) {
                if ( v1[i] != 0 && v2[i] != 0 ) {
                    dist += v1[i] * log2( v1[i] / v2[i] );
                }
            }
            return dist;
        }

        // note: vectors have to be normalized to be able to be treated as probability distribution
        public static float calcJeffreyDistance ( float[] v1, float[] v2 ) {
            int max = v1.length;
            if ( max != v2.length ) {
                throw new UnsupportedOperationException( "length mismatch: " + v1.length + " != " + v2.length );
            }
            float dist = 0, m;
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

        public static final float LN_2 = (float) Math.log( 2 );

        public static float log2 ( float f ) {
            return (float) Math.log( f ) / LN_2;
        }

        private Distance () {
            throw new UnsupportedOperationException();
        }
    }
}
