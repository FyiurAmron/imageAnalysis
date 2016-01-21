package vax.gfx;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import jdk.nashorn.internal.objects.NativeDate;
import static vax.gfx.Main.packRGB;

/**

 @author toor
 */
public class HistoTest {
    public static void fill ( int[] array, int value ) {
        int len = array.length;

        if ( len > 0 ) {
            array[0] = value;
        }

        for( int i = 1; i < len; i += i ) {
            System.arraycopy( array, 0, array, i, ( ( len - i ) < i ) ? ( len - i ) : i );
        }
    }

    public static class HistoImage {
        private final BufferedImage histoImage = new BufferedImage( 256, 256, BufferedImage.TYPE_INT_ARGB );
        private final ImageLabel imageLabel = new ImageLabel( histoImage );
        private final int[] valueCounter = new int[1 << Byte.SIZE];
        private int valueCount, counterMax;
        private int color;

        public HistoImage () {
            this( 0xFF808080 );
        }

        public HistoImage ( int color ) {
            this.color = color;
        }

        public void count ( int value ) {
            valueCount++;
            valueCounter[value]++;
            if ( valueCounter[value] > counterMax ) {
                counterMax = valueCounter[value];
            }
        }

        public void reset () {
            valueCount = 0;
            counterMax = 0;
            fill( valueCounter, 0 );
        }

        public int getCounterMax () {
            return counterMax;
        }

        public float[] getProbabilityDistribution () {
            return getProbabilityDistribution( new float[valueCounter.length] );
        }

        public float[] getProbabilityDistribution ( float[] target ) {
            for( int i = valueCounter.length - 1; i > 0; i-- ) {
                target[i] = valueCounter[i] / valueCount;
            }
            return target;
        }

        /*
         public BufferedImage getHistoImage () {
         return histoImage;
         }
         */
        public ImageLabel getImageLabel () {
            return imageLabel;
        }

        public void updateImage () {
            updateImage( counterMax );
        }

        public void updateImage ( int counterMax ) {
            histoImage.getGraphics().clearRect( 0, 0, 256, 256 );
            for( int i = 0, max = valueCounter.length; i < max; i++ ) {
                histoImage.setRGB( i, 0, 0xFFFFFFFF );
                histoImage.setRGB( i, 255, 0xFFFFFFFF );
                histoImage.setRGB( 0, i, 0xFFFFFFFF );
                histoImage.setRGB( 255, i, 0xFFFFFFFF );
                int value = 255 - 255 * valueCounter[i] / counterMax;
                for( int y = 255; y >= value; y-- ) {
                    histoImage.setRGB( i, y, color );
                }
            }
        }
    }

    public static void testHisto ( Container container1, Container container2 ) throws FileNotFoundException, IOException {
        PNG.Decoder dec = new PNG.Decoder( new FileInputStream( "img/0001.png" ) );
        int width = dec.getWidth(), height = dec.getHeight();
        BufferImage bi = new BufferImage( width, height, dec.getBytesPerPixel() );
        ByteBuffer bb = bi.buffer;
        //IntBuffer ib = bb.asIntBuffer();
        dec.decode( bb, width * 3, PNG.Format.RGB );
        bb.rewind();
        int[] imgData = new int[width * height];
        int[] imgDataHSV = new int[width * height];
        float[] hsb = new float[3];
        //HistoImage histoRGB = new HistoImage();
        HistoImage histoR = new HistoImage( ( 0xFF << 16 ) | ( 0xFF << 24 ) );
        HistoImage histoG = new HistoImage( ( 0xFF << 8 ) | ( 0xFF << 24 ) );
        HistoImage histoB = new HistoImage( ( 0xFF ) | ( 0xFF << 24 ) );
        HistoImage histoGray = new HistoImage();

        for( int i = 0, max = imgData.length; i < max; i++ ) {
            byte r = bb.get(), g = bb.get(), b = bb.get();
            int iR = Byte.toUnsignedInt( r ), iG = Byte.toUnsignedInt( g ), iB = Byte.toUnsignedInt( b );
            histoR.count( iR );
            histoG.count( iG );
            histoB.count( iB );
            histoGray.count( ( iR + iG + iB ) / 3 );
            imgData[i] = packRGB( r, g, b );
            Color.RGBtoHSB( r, g, b, hsb );
            //hsb[0] = 0;
            //hsb[1] = 0;
            //hsb[2] = 0;
            /*
             imgDataHSV[i] = ( ( (int) ( hsb[0] * 255 ) ) << 16 )
             | ( ( (int) ( hsb[1] * 255 ) ) << 8 )
             | ( (int) ( hsb[2] * 255 ) );
             */
            imgDataHSV[i] = ( ( (int) ( hsb[1] * 255 ) ) & 0xFF );
        }

        int maxCounterMax = Math.max( histoR.getCounterMax(),
                Math.max( histoG.getCounterMax(), histoB.getCounterMax() ) );
        histoR.updateImage( maxCounterMax );
        histoG.updateImage( maxCounterMax );
        histoB.updateImage( maxCounterMax );
        histoGray.updateImage( maxCounterMax );

        BufferedImage jbi1 = new BufferedImage( width, height, BufferedImage.TYPE_3BYTE_BGR );
        //BufferedImage jbi2 = new BufferedImage( width, height, BufferedImage.TYPE_3BYTE_BGR );

        jbi1.setRGB( 0, 0, dec.getWidth(), dec.getHeight(), imgData, 0, width );
        //jbi2.setRGB( 0, 0, dec.getWidth(), dec.getHeight(), imgDataHSV, 0, width );

        ImageLabel il1 = new ImageLabel( jbi1 );
        //ImageLabel il2 = new ImageLabel( jbi2 );
        //ImageLabel il2 = new ImageLabel( histoRGB );
        ImageLabel il3 = histoR.getImageLabel();
        ImageLabel il4 = histoG.getImageLabel();
        ImageLabel il5 = histoB.getImageLabel();
        ImageLabel il6 = histoGray.getImageLabel();
        Component[] imageViews = new Component[]{ /* il1, */ il3, il4, il5, il6 };
        Dimension minSize = new Dimension( 256, 256 );
        il1.setMinimumSize( minSize );
        container1.add( il1 );
        for( Component c : imageViews ) {
            c.setMinimumSize( minSize );
            container2.add( c );
        }

        Main.Counter cnt = new Main.Counter();

        container2.add( new Main.VaxButton( "toggle", (ActionEvent t) -> {
            Main.maskByteBuffer( bi, imgData, jbi1, 0xFF << cnt.next() );
            histoR.reset();
            histoG.reset();
            histoB.reset();
            histoGray.reset();
            bb.rewind();
            for( int i = 0, max = imgData.length; i < max; i++ ) {
                byte r = bb.get(), g = bb.get(), b = bb.get();
                int iR = Byte.toUnsignedInt( r ), iG = Byte.toUnsignedInt( g ), iB = Byte.toUnsignedInt( b );
                histoR.count( iR );
                histoG.count( iG );
                histoB.count( iB );
                histoGray.count( ( iR + iG + iB ) / 3 );
                imgData[i] = packRGB( r, g, b );
                Color.RGBtoHSB( r, g, b, hsb );
                //hsb[0] = 0;
                //hsb[1] = 0;
                //hsb[2] = 0;
                /*
                 imgDataHSV[i] = ( ( (int) ( hsb[0] * 255 ) ) << 16 )
                 | ( ( (int) ( hsb[1] * 255 ) ) << 8 )
                 | ( (int) ( hsb[2] * 255 ) );
                 */
                imgDataHSV[i] = ( ( (int) ( hsb[1] * 255 ) ) & 0xFF );
            }
            int cntr = Math.max( histoR.getCounterMax(),
                    Math.max( histoG.getCounterMax(), histoB.getCounterMax() ) );
            histoR.updateImage( cntr );
            histoG.updateImage( cntr );
            histoB.updateImage( cntr );
            histoGray.updateImage( cntr );
            il1.repaint();
            for( Component c : imageViews ) {
                c.repaint();
            }
        } ) );
    }
}
