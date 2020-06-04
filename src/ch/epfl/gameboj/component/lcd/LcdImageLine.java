package ch.epfl.gameboj.component.lcd;

import static ch.epfl.gameboj.Preconditions.checkArgument;
import static ch.epfl.gameboj.Preconditions.checkBits8;

import java.util.Objects;

import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.bits.Bits;

public final class LcdImageLine {
    
    //CONSTANTS
    private static final int COLOR_NUM = 4;
    private static final int IDLE_PALETTE = 0b1110_0100;
    
    //FIELDS
    private final BitVector msb;
    private final BitVector lsb;
    private final BitVector opacity;
    

    
    /**
     * @param msb most significant bits of pixels
     * @param lsb least significant bits of pixels
     * @param opacity of pixels
     * checks if they are of same length and then constructs the line
     */
    public LcdImageLine(BitVector msb, BitVector lsb, BitVector opacity) {
        checkArgument(msb.size() == lsb.size() && lsb.size() == opacity.size());

        this.msb = msb;
        this.lsb = lsb;
        this.opacity = opacity;

    }

    /**
     * @return the size of line, since msb, lsb and opacity are of same size we simply 
     * return the size of msb
     */
    public int size() {
        return msb.size();
    }

    /**
     * @return the most significant bits of pixels
     */
    public BitVector msb() {
        return msb;
    }

    /**
     * @return the least significant bits of pixels
     */
    public BitVector lsb() {
        return lsb;
    }

    /**
     * @return the opacity of pixels
     */
    public BitVector opacity() {
        return opacity;
    }

    /**
     * @param shiftDistance : distance we want to shift
     * @return the line constructed with shifted msb, lsb and opacity
     */
    public LcdImageLine shift(int shiftDistance) {
        BitVector shiftedMsb = msb.shift(shiftDistance);
        BitVector shiftedLsb = lsb.shift(shiftDistance);
        BitVector shiftedOpacity = opacity.shift(shiftDistance);

        return new LcdImageLine(shiftedMsb, shiftedLsb, shiftedOpacity);
    }

    /**
     * @param index : index from which we extract 
     * @param distance : length of bits we extract
     * @return the line constructed with (wrapped) extracted  msb, lsb and opacity
     */
    public LcdImageLine extractWrapped(int index, int distance) {
        BitVector extractedMsb = msb.extractWrapped(index, distance);
        BitVector extractedLsb = lsb.extractWrapped(index, distance);
        BitVector extractedOpacity = opacity.extractWrapped(index, distance);

        return new LcdImageLine(extractedMsb, extractedLsb, extractedOpacity);

    }
    
    /**
     * @param index from which we extract
     * @param distance :length of bits we want to extract 
     * @return the line constructed with (zero extended) extracted msb, lsb and opacity
     */
    public LcdImageLine extractZeroExtended(int index, int distance) {
        BitVector extractedMsb = msb.extractZeroExtended(index, distance);
        BitVector extractedLsb = lsb.extractZeroExtended(index, distance);
        BitVector extractedOpacity = opacity.extractZeroExtended(index, distance);

        return new LcdImageLine(extractedMsb, extractedLsb, extractedOpacity);

    }
 

    public LcdImageLine mapColors(int palette) {
        checkBits8(palette); //validating the arguments
        //case the colors doesn't change
        if (palette == IDLE_PALETTE) {
            return this;
        }
        
        //local variable to store temporary color
        int color;
        
        //array stores the BitVectors with appropriate locations
        BitVector[] locations = new BitVector[COLOR_NUM];
        
        //BitVectors represents new lsb and msb variables and their complements
        BitVector newMsb = new BitVector(msb.size());
        BitVector newLsb = new BitVector(lsb.size());
        BitVector complementMsb = msb.not();
        BitVector complementLsb = lsb.not();

        //setting the locations of the BitVectors
        locations[0b00] = complementMsb.and(complementLsb);
        locations[0b01] = complementMsb.and(lsb);
        locations[0b10] = msb.and(complementLsb);
        locations[0b11] = msb.and(lsb);

        for (int i = 0; i < locations.length; ++i) {
            //extracting the last two bits of the palette
            color = Bits.extract(palette, 0, 2);
            //updating the msb
            if (Bits.test(color, 1)) {
                newMsb = newMsb.or(locations[i]);
            }

            //updating the msb
            if (Bits.test(color, 0)) {
                newLsb = newLsb.or(locations[i]);
            }
            //shifting the palette by two bits
            palette = palette >>> 2;
        }
        return new LcdImageLine(newMsb, newLsb, opacity);
    }

    
    /**
     * @param top: line on top
     * @param below : line below
     * @return superpositioned line by calling below method giving top line's opacity 
     * for the opacityRef(opacity reference)
     */
    public static LcdImageLine below(LcdImageLine top, LcdImageLine below) {
        return below(top, below, top.opacity);

    }

    /**
     * @param top : line on top
     * @param below : line below
     * @param opacityRef : according to this opacity reference we are
     * able to choose which pixel to show (the only way we see the pixel of below line
     * is that opacity reference is 0)
     * @return the line with proper msb, lsb and opacity after superposition
     */
    public static LcdImageLine below(LcdImageLine top, LcdImageLine below,
            BitVector opacityRef) {
        checkArgument(top.size() == below.size());

        BitVector resultMsb = top.msb.and(opacityRef).or((below.msb.and(opacityRef.not())));
        BitVector resultLsb = top.lsb.and(opacityRef).or((below.lsb.and(opacityRef.not())));
        BitVector resultOpacity = opacityRef.or(below.opacity);

        return new LcdImageLine(resultMsb, resultLsb, resultOpacity);
    }

  /**
 * @param firstLine 
 * @param secondLine
 * @param pixelsFromFirstLine : number of pixels to get from firstLine
 * we compute the number of pixels to get from second line(pixelsFromSecondLine)
 * with that and the parameter pixelsFromFirst line we do shifting to both lines to 
 * make those pixels zero in order to concatenate them with a simple disjunction
 * @return the constructed line with joint msb, lsb and opacity
 */
public static LcdImageLine join(LcdImageLine firstLine ,LcdImageLine secondLine, int pixelsFromFirstLine) {
  checkArgument(pixelsFromFirstLine >= 0 && pixelsFromFirstLine <= firstLine.size() && firstLine.size() == secondLine.size());

  int pixelsFromSecondLine = firstLine.size() - pixelsFromFirstLine;
  LcdImageLine shiftedLine1;
  LcdImageLine shiftedLine2;
  
  shiftedLine1 = firstLine.shift(-pixelsFromSecondLine);
  shiftedLine1 = shiftedLine1.shift(pixelsFromSecondLine);

  shiftedLine2 = secondLine.shift(pixelsFromFirstLine);
  shiftedLine2 = shiftedLine2.shift(-pixelsFromFirstLine);

  BitVector jointMsb = shiftedLine1.msb.or(shiftedLine2.msb);
  BitVector jointLsb = shiftedLine1.lsb.or(shiftedLine2.lsb);
  BitVector jointOpacity = shiftedLine1.opacity.or(shiftedLine2.opacity);

  return new LcdImageLine(jointMsb, jointLsb, jointOpacity);

}

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     * calls the hash method with all fields that this object has
     * in order to get a specific hashing 
     */
    @Override
    public int hashCode() {
        return Objects.hash(msb, lsb, opacity);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     * controls equality of lines with the hashing formed above
     */
    @Override
    public boolean equals(Object thatLine) {
        return this.hashCode() == thatLine.hashCode();
    }

    //BUILDER INNER CLASS   
    public static final class Builder {

        //FIELDS
        private final BitVector.Builder msbBuilder;
        private final BitVector.Builder lsbBuilder;

        //CONSTRUCTOR
        /**
         * @param size: length of the line to be created
         * instantiates the BitVector builders
         */
        public Builder(int size) {
            msbBuilder = new BitVector.Builder(size);
            lsbBuilder = new BitVector.Builder(size);
        }
        
        //METHODS
        /**
         * @param index the index of the pixel wanted to changed in the line
         * @param msbValue new msb value of the index in the line
         * @param lsbValue new lsb value of the index int the line 
         * @return the builder itself after setting the bytes of the fields
         */
        public Builder setBytes(int index, int msbValue, int lsbValue) {
            msbBuilder.setByte(index, msbValue);
            lsbBuilder.setByte(index, lsbValue);
            return this;
        }

        /**
         * setting the msb and the lsb, then
         * @return a new LcdImageLine whose opacity is the 
         * disjunction of the msb and lsb
         */
        public LcdImageLine build() {
            BitVector msb = msbBuilder.build();
            BitVector lsb = lsbBuilder.build();
            BitVector opacity = msb.or(lsb);
            return new LcdImageLine(msb, lsb, opacity);
        }
    }

}
