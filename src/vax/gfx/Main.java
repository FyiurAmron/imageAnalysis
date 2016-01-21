package vax.gfx;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.*;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import javax.swing.*;

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
        Container cp = jf.getContentPane();
        //cp.setBackground( Color.BLACK );
        cp.setLayout( new BoxLayout( cp, BoxLayout.Y_AXIS ) );
        JPanel jp1 = new JPanel(), jp2 = new JPanel(), jp3 = new JPanel();
        jp2.setBackground( Color.DARK_GRAY );
        jp3.setBackground( Color.DARK_GRAY );
        cp.add( jp1 );
        cp.add( jp2 );
        cp.add( jp3 );
        //jf.setLayout( new FlowLayout() );
        boolean histoTest = true;

        if ( histoTest ) {
            HistoTest.testHisto( jp1, jp2, jp3 );
        } else {
            args = new String[]{ "img/0001.png", /* "1000" */ "13", "4" }; // TEMP!

            if ( args.length < 3 ) {
                System.out.println( "not enough parameters!" );
                return;
            }

            String sourcePatternFilename = args[0];
            int searchCount = Integer.valueOf( args[1] );
            int bitsPerComponent = Integer.valueOf( args[2] );

            MatchTest.testMatching( sourcePatternFilename, searchCount, bitsPerComponent );
        }
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

    public static BufferedImage toBufferedImageMask ( BufferImage bi, int[] tempBuffer, BufferedImage jbi, int mask ) {
        ByteBuffer bb = bi.buffer;
        bb.rewind();
        for( int i = 0, max = tempBuffer.length; i < max; i++ ) {
            byte r = (byte) ( bb.get() & mask ), g = (byte) ( bb.get() & mask ), b = (byte) ( bb.get() & mask );
            tempBuffer[i] = packRGB( r, g, b );
        }
        jbi.setRGB( 0, 0, bi.width, bi.height, tempBuffer, 0, bi.width );
        return jbi;
    }

    public static BufferedImage maskByteBuffer ( BufferImage bi, int[] tempBuffer, BufferedImage jbi, int mask ) {
        return maskByteBuffer( bi, tempBuffer, jbi, (a, b, c) -> a & mask, (a, b, c) -> b & mask, (a, b, c) -> c & mask );
    }

    public interface IntTripletFunction {
        int accept ( int a, int b, int c );
    }

    public static BufferedImage maskByteBuffer ( BufferImage bi, int[] tempBuffer, BufferedImage jbi,
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
            tempBuffer[i] = packRGB( r, g, b );
        }
        jbi.setRGB( 0, 0, bi.width, bi.height, tempBuffer, 0, bi.width );
        return jbi;
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
