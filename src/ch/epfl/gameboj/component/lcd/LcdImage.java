package ch.epfl.gameboj.component.lcd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.BitVector;

public final class LcdImage {

    //CONSTANTS
    private final static int WINDOW_HEIGHT = 256;
    private final static int WINDOW_LENGTH = 256;
    //FIELDS
    private final int width;
    private final int height;
    private final List<LcdImageLine> imageList;
    
    //CONSTRUCTORS 
    /**
     * @param width: width of the image in pixels
     * @param height: height of the image in pixels
     * @param imageList: list of lines of the image
     */
    public LcdImage(int width, int height, List<LcdImageLine> imageList) {
        Preconditions.checkArgument(width >= 0 && height >= 0 && imageList.size() == height);      
        this.width = width;
        this.height = height;
        this.imageList = Collections.unmodifiableList(imageList);
    }
    
    /**
     * @return the width of the image
     */
    public int width() {
        return width;
    }
    
    /**
     * @return the height of the image
     */
    public int height() {
        return height;
    }
    
    /**
     * @param x: x coordinate of the pixel
     * @param y: y coordinate of the pixel
     * @return: pixel's msb, lsb value
     */
    public int get(int x , int y) {
        Preconditions.checkArgument(x >= 0 && x < WINDOW_LENGTH && y >= 0 && y < WINDOW_HEIGHT);
        LcdImageLine wantedLine = imageList.get(y);
        boolean colorMsb = wantedLine.msb().testBit(x);
        boolean colorLsb = wantedLine.lsb().testBit(x);
        int bitMsb = colorMsb ? 1 : 0;
        int bitLsb = colorLsb ? 1 : 0;
        return (bitMsb << 1) + bitLsb ;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     * returns hashed value of all the fields
     */
    @Override
    public int hashCode() {
        return Objects.hash(width, height, imageList);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     * compares the hash codes of the object and the parameter
     */
    @Override
    public boolean equals(Object that) {
       return this.hashCode() == that.hashCode();
    }
    
    //BUILDER INNER CLASS
    public static final class Builder{
        private List<LcdImageLine> list;
        
        /**
         * @param width: width of the image to be build in pixels
         * @param height: height of the image to be build in pixels
         * creates an empty image
         */
        public Builder(int width, int height) {
            Preconditions.checkArgument(height > 0 && height < WINDOW_HEIGHT);  //width's condition is checked in BitVector
            list = new ArrayList<>(height);
            for(int i = 0 ; i < height ; ++i) {
                list.add(new LcdImageLine(new BitVector(width),new BitVector(width),new BitVector(width)));
            }
        }
        
        /**
         * @param index: index of line in the list of lines
         * @param line: new line to set
         * @return: sets the line and returns the builder
         */
        public Builder setLine(int index, LcdImageLine line){
            Preconditions.checkArgument(index >= 0 && index < list.size());
            list.set(index, line);
            return this;
        }
      
        
        /**
         * @return: the image builded
         */
        public LcdImage build() {
            int width = list.get(0).size();
            int height = list.size();
            return new LcdImage(width, height, list);
        }  
        
    }

}
