package vax.gfx;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.image.*;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import javax.swing.*;
import vax.gfx.PNG.Decoder;

/**

 @author toor
 */
public class Main {

    /**
     @param args the command line arguments
     @throws java.lang.Exception
     */
    public static void main ( String[] args ) throws Exception {
        JFrame jf = new JFrame( "imageAnalysis" );
        jf.getContentPane().setBackground( Color.BLACK );
        jf.setLayout( new FlowLayout() );

        Decoder dec = new PNG.Decoder( new FileInputStream( "img/0001.png" ) );
        int width = dec.getWidth(), height = dec.getHeight();
        BufferImage bi = new BufferImage( width, height, dec.getBytesPerPixel() );
        ByteBuffer bb = bi.buffer;
        //IntBuffer ib = bb.asIntBuffer();
        dec.decode( bb, width * 3, PNG.Format.RGB );
        bb.rewind();
        int[] imgData = new int[width * height];
        int[] imgDataHSV = new int[width * height];
        float[] hsb = new float[3];
        int[][] valueCounter = new int[4][1 << Byte.SIZE];
        int countMax = 0;
        BufferedImage histoRGB = new BufferedImage( 256, 256, BufferedImage.TYPE_INT_ARGB );
        BufferedImage histoR = new BufferedImage( 256, 256, BufferedImage.TYPE_INT_ARGB );
        BufferedImage histoG = new BufferedImage( 256, 256, BufferedImage.TYPE_INT_ARGB );
        BufferedImage histoB = new BufferedImage( 256, 256, BufferedImage.TYPE_INT_ARGB );
        BufferedImage histoGray = new BufferedImage( 256, 256, BufferedImage.TYPE_INT_ARGB );

        for( int i = 0, max = imgData.length; i < max; i++ ) {
            byte r = bb.get(), g = bb.get(), b = bb.get();
            int ir = Byte.toUnsignedInt( r ), ig = Byte.toUnsignedInt( g ), ib = Byte.toUnsignedInt( b );
            if ( ++valueCounter[0][ir] > countMax ) {
                countMax = valueCounter[0][ir];
            }
            if ( ++valueCounter[1][ig] > countMax ) {
                countMax = valueCounter[1][ig];
            }
            if ( ++valueCounter[2][ib] > countMax ) {
                countMax = valueCounter[2][ib];
            }
            int gray = ( ir + ig + ib ) / 3;
            valueCounter[3][gray]++;
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
        for( int i = 0, max = valueCounter[0].length; i < max; i++ ) {
            histoR.setRGB( i, 0, 0xFFFFFFFF );
            histoR.setRGB( i, 255, 0xFFFFFFFF );
            histoR.setRGB( 0, i, 0xFFFFFFFF );
            histoR.setRGB( 255, i, 0xFFFFFFFF );

            histoG.setRGB( i, 0, 0xFFFFFFFF );
            histoG.setRGB( i, 255, 0xFFFFFFFF );
            histoG.setRGB( 0, i, 0xFFFFFFFF );
            histoG.setRGB( 255, i, 0xFFFFFFFF );

            histoB.setRGB( i, 0, 0xFFFFFFFF );
            histoB.setRGB( i, 255, 0xFFFFFFFF );
            histoB.setRGB( 0, i, 0xFFFFFFFF );
            histoB.setRGB( 255, i, 0xFFFFFFFF );

            histoGray.setRGB( i, 0, 0xFFFFFFFF );
            histoGray.setRGB( i, 255, 0xFFFFFFFF );
            histoGray.setRGB( 0, i, 0xFFFFFFFF );
            histoGray.setRGB( 255, i, 0xFFFFFFFF );

            int rValue = 255 - 255 * valueCounter[0][i] / countMax,
                    gValue = 255 - 255 * valueCounter[1][i] / countMax,
                    bValue = 255 - 255 * valueCounter[2][i] / countMax,
                    grayValue = 255 - 255 * valueCounter[3][i] / countMax;
            for( int y = 255; y >= rValue; y-- ) {
                histoR.setRGB( i, y, ( 0xFF << 16 ) | ( 0xFF << 24 ) );
            }
            for( int y = 255; y >= gValue; y-- ) {
                histoG.setRGB( i, y, ( 0xFF << 8 ) | ( 0xFF << 24 ) );
            }
            for( int y = 255; y >= bValue; y-- ) {
                histoB.setRGB( i, y, ( 0xFF ) | ( 0xFF << 24 ) );
            }
            for( int y = 255; y >= grayValue; y-- ) {
                histoGray.setRGB( i, y, 0xFF808080 );
            }
            /*
             histoJBI.setRGB( i, rValue, ( 0xFF << 16 ) | ( 0xFF << 24 ) );
             histoJBI.setRGB( i, gValue, ( 0xFF << 8 ) | ( 0xFF << 24 ) );
             histoJBI.setRGB( i, bValue, ( 0xFF ) | ( 0xFF << 24 ) );
             */
        }

        BufferedImage jbi1 = new BufferedImage( width, height, BufferedImage.TYPE_3BYTE_BGR );
        //BufferedImage jbi2 = new BufferedImage( width, height, BufferedImage.TYPE_3BYTE_BGR );

        jbi1.setRGB( 0, 0, dec.getWidth(), dec.getHeight(), imgData, 0, width );
        //jbi2.setRGB( 0, 0, dec.getWidth(), dec.getHeight(), imgDataHSV, 0, width );

        ImageLabel il1 = new ImageLabel( jbi1 );
        //ImageLabel il2 = new ImageLabel( jbi2 );
        //ImageLabel il2 = new ImageLabel( histoRGB );
        ImageLabel il3 = new ImageLabel( histoR );
        ImageLabel il4 = new ImageLabel( histoG );
        ImageLabel il5 = new ImageLabel( histoB );
        ImageLabel il6 = new ImageLabel( histoGray );

        jf.add( il1 );
        //jf.add( il2 );
        jf.add( il3 );
        jf.add( il4 );
        jf.add( il5 );
        jf.add( il6 );

        Counter cnt = new Counter();

        jf.add( new VaxButton( "toggle", (ActionEvent t) -> {
            //toBufferedImageMask( bi, jbi1, imgData, 0x7F >> cnt.next() );
            toBufferedImageMask( bi, jbi1, imgData, 0xFF << cnt.next() );
            il1.repaint();
        } ) );
        jf.pack();
        jf.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        jf.setVisible( true );
    }

    public static class Counter {
        private int i;

        public Counter ( int init ) {
            this.i = init;
        }

        public Counter () {
            this( 0 );
        }

        public int next () {
            i++;
            return i;
        }
    }

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

    public static BufferedImage toBufferedImageMask ( BufferImage bi, BufferedImage jbi, int[] tempBuffer, int mask ) {
        ByteBuffer bb = bi.buffer;
        bb.rewind();
        for( int i = 0, max = tempBuffer.length; i < max; i++ ) {
            byte r = (byte) ( bb.get() & mask ), g = (byte) ( bb.get() & mask ), b = (byte) ( bb.get() & mask );
            tempBuffer[i] = packRGB( r, g, b );
        }
        jbi.setRGB( 0, 0, bi.width, bi.height, tempBuffer, 0, bi.width );
        return jbi;
    }

    public static class ImageLabel extends JLabel {
        private ImageIcon icon;

        public ImageLabel ( Image image ) {
            super();
            icon = new ImageIcon( image );
            setIcon( icon );
        }

        public void setImage ( Image image ) {
            icon.setImage( image );
        }
    }

    public static class VaxButton extends JButton {
        public VaxButton ( Consumer<ActionEvent> eventConsumer ) {
            super();
            addEventConsumer( eventConsumer );
        }

        public VaxButton ( String caption, Consumer<ActionEvent> eventConsumer ) {
            super( caption );
            addEventConsumer( eventConsumer );
        }

        public final void addEventConsumer ( Consumer<ActionEvent> eventConsumer ) {
            addActionListener( (ActionEvent e) -> eventConsumer.accept( e ) );
        }
    }

    public static int packRGB ( int r, int g, int b ) {
        return ( ( r & 0xFF ) << 16 ) | ( ( g & 0xFF ) << 8 ) | ( b & 0xFF );
    }

}
