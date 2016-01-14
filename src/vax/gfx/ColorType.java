package vax.gfx;

/**
 Note: allowed bitDepths for PNG are specified in enum constant docs.

 @author toor
 */
public enum ColorType {
    /** 1, 2, 4, 8, 16 */
    Grays( 0 ),
    /** 8, 16 */
    RGB( 2 ),
    /** 1, 2, 4, 8 ; note that palette entries are 8-bit themselves */
    Palette( 3 ),
    /** 8, 16 */
    GraysA( 4 ),
    /** 8, 16 */
    RGBA( 6 );

    private final int pngConstant;

    private ColorType ( int pngConstant ) {
        this.pngConstant = pngConstant;
    }

    public int getPngConstant () {
        return pngConstant;
    }

    public static ColorType fromComponentCount ( int componentCount ) {
        switch ( componentCount ) {
            case 4:
                return RGBA;
            case 3:
                return RGB;
            case 2:
                return GraysA;
            case 1:
                return Grays;
            default:
                throw new IllegalArgumentException( "componentCount == '" + componentCount + "' out of range (1,4)" );
        }
    }

}
