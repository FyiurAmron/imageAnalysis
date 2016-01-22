package vax.gfx;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;
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
        boolean histoTest = false;

        args = new String[]{ "img/test2.png", /* "1000" */ "13", "4" }; // TEMP!
        if ( histoTest ) {
            HistoTest.testHisto( args[0], jp1, jp2, jp3 );
        } else {
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
