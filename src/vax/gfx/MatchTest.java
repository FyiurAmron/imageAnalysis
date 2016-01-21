package vax.gfx;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**

 @author toor
 */
public class MatchTest {
    public static void testMatching ( String sourcePatternFilename, int searchCount, int bitsPerComponent ) throws IOException {
        PNG.Decoder dec = new PNG.Decoder( new FileInputStream( sourcePatternFilename ) );
        int width = dec.getWidth(), height = dec.getHeight();
        BufferImage bi = new BufferImage( width, height, dec.getBytesPerPixel() );
        ByteBuffer bb = bi.buffer;
        dec.decode( bb, width * 3, PNG.Format.RGB );
        int possibleComponentValues = 1 << bitsPerComponent;
        possibleComponentValues *= possibleComponentValues;
        possibleComponentValues *= possibleComponentValues;
        float[] output = new float[possibleComponentValues];
        int[] helper = new int[possibleComponentValues];
        ImageAnalysis.Histogram.calcHistogram3( bb, output, possibleComponentValues, helper );
    }

    private MatchTest () {
        throw new UnsupportedOperationException();
    }
}
