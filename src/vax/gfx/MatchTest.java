package vax.gfx;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import vax.gfx.ImageAnalysis.Distance;

/**

 @author toor
 */
public class MatchTest {
    public static void calcHistogram ( ByteBuffer bb, double[] output, int possibleComponentValues, int[] rawCountOutput, int histMode ) {
        switch ( histMode ) {
            case 0:
                ImageAnalysis.Histogram.calcHistogram3( bb, output, possibleComponentValues, rawCountOutput );
                break;
            case 1:
                ImageAnalysis.Histogram.calcHistogramHSB3( bb, output, possibleComponentValues, rawCountOutput );
                break;
            case 2:
                ImageAnalysis.Histogram.calcHistogramHue( bb, output, possibleComponentValues, rawCountOutput );
                break;
        }
        throw new IllegalArgumentException();
    }

    public static double calcDistance ( double[] v1, double[] v2, int distMode ) {
        switch ( distMode ) {
            case 0:
                return ImageAnalysis.Distance.calcManhattanDistance( v1, v2 );
            case 1:
                return ImageAnalysis.Distance.calcEuclideanDistance( v1, v2 );
            case 2:
                return ImageAnalysis.Distance.calcCosineDistance( v1, v2 );
            case 3:
                return ImageAnalysis.Distance.calcIntersection( v1, v2 );
            case 4:
                return ImageAnalysis.Distance.calcNormalizedCrossCorrelation( v1, v2 );
            case 5:
                return ImageAnalysis.Distance.calcKLDistance( v1, v2 );
            case 6:
                return ImageAnalysis.Distance.calcJeffreyDistance( v1, v2 );
        }
        throw new IllegalArgumentException();
    }

    public static void testMatching ( String sourcePatternFilename, int searchCount, int bitsPerComponent, int distMode, int histMode )
            throws IOException {
        int possibleComponentValues = 1 << bitsPerComponent;
        int dim = possibleComponentValues * possibleComponentValues * possibleComponentValues;
        double[] output = new double[dim];
        int[] helper = new int[dim];

        BufferImage bi;
        ByteBuffer bb;
        try ( FileInputStream fis = new FileInputStream( sourcePatternFilename ) ) {
            PNG.Decoder dec = new PNG.Decoder( fis );
            int width = dec.getWidth(), height = dec.getHeight();
            bi = new BufferImage( width, height, dec.getBytesPerPixel() );
            bb = bi.buffer;
            dec.decode( bb, width * 3, PNG.Format.RGB );
        }
        ImageAnalysis.maskByteBuffer( bi, null,
                (a, b, c) -> a >> ( 8 - bitsPerComponent ),
                (a, b, c) -> b >> ( 8 - bitsPerComponent ),
                (a, b, c) -> c >> ( 8 - bitsPerComponent ) ); // shift mask
        calcHistogram( bb, output, possibleComponentValues, helper, histMode );
        double[] distances = new double[searchCount];
        for( int i = 0; i < searchCount; i++ ) {
            BufferImage bi2;
            ByteBuffer bb2;
            try ( FileInputStream fis = new FileInputStream( "img/" + String.format( Locale.ROOT, "%04d", i + 1 ) + ".png" ) ) {
                PNG.Decoder dec2 = new PNG.Decoder( fis );
                int width2 = dec2.getWidth(), height2 = dec2.getHeight();
                bi2 = new BufferImage( width2, height2, dec2.getBytesPerPixel() );
                bb2 = bi2.buffer;
                dec2.decode( bb2, width2 * 3, PNG.Format.RGB );
            }
            ImageAnalysis.maskByteBuffer( bi2, null,
                    (a, b, c) -> a >> ( 8 - bitsPerComponent ),
                    (a, b, c) -> b >> ( 8 - bitsPerComponent ),
                    (a, b, c) -> c >> ( 8 - bitsPerComponent ) ); // shift mask
            double[] out2 = new double[dim];
            calcHistogram( bb2, out2, possibleComponentValues, helper, histMode );
            ImageAnalysis.Histogram.calcHistogram3( bb2, out2, possibleComponentValues, helper );
            distances[i] = calcDistance( output, out2, distMode );
        }
        double distMin = Double.POSITIVE_INFINITY;
        int distMinI = -1;
        for( int i = 0; i < searchCount; i++ ) {
            if ( distances[i] < distMin ) {
                distMin = distances[i];
                distMinI = i;
            }
        }
        System.out.println( "min distance: i = " + distMinI + " (file '" + String.format( Locale.ROOT, "%04d", distMinI + 1 )
                + "') dist = " + distMin + "\nother matches:" );
        for( int i = 0; i < searchCount; i++ ) {
            System.out.println( "i = " + i + " dist[i] = " + distances[i] );
        }
        //double[] t = ImageAnalysis.Correlogram.generate( possibleComponentValues, bi );
    }

    private MatchTest () {
        throw new UnsupportedOperationException();
    }
}
