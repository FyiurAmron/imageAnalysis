package vax.gfx;

import java.awt.event.ActionEvent;
import java.util.function.Consumer;
import javax.swing.*;

/**

 @author toor
 */
public class Main {
    private static JFrame appFrame, controlFrame, histoFrame;

    /**
     @param args the command line arguments
     @throws java.lang.Exception
     */
    public static void main ( String[] args ) throws Exception {
        if ( args.length == 0 ) {
            appFrame = new JFrame( "imageAnalysis" );
            controlFrame = new JFrame( "controls" );
            histoFrame = new JFrame( "histograms" );

            HistoTest.testHisto( "img/test2.png", appFrame, histoFrame, controlFrame );

            appFrame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
            appFrame.setVisible( true );

            histoFrame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
            histoFrame.setVisible( true );

            controlFrame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
            controlFrame.setAlwaysOnTop( true );
            controlFrame.setVisible( true );
            return;
        }

        if ( args.length < 3 ) {
            System.out.println( "not enough parameters!" );
            return;
        }

        String sourcePatternFilename = args[0];
        int searchCount = Integer.valueOf( args[1] );
        int bitsPerComponent = Integer.valueOf( args[2] );
        int histMode = ( args.length > 3 ) ? Integer.valueOf( args[3] ) : 0;
        int distMode = ( args.length > 4 ) ? Integer.valueOf( args[4] ) : 0;

        MatchTest.testMatching( sourcePatternFilename, searchCount, bitsPerComponent, histMode, distMode );
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
