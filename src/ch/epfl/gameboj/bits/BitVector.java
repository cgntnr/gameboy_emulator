package ch.epfl.gameboj.bits;

import static ch.epfl.gameboj.Preconditions.checkArgument;
import static ch.epfl.gameboj.Preconditions.checkBits8;

import java.util.Arrays;
import java.util.Objects;

public final class BitVector {
    
    
    //CONSTANTS
    private static final int ALL_ONES = -1;  // =  0b11111111_11111111_11111111_11111111

    //FIELDS
    private final int[] bits;
    
    //ENUM OF EXTRACT
    private enum ExtractType{
        WRAPPED, ZERO_EXTENDED
    }
   
    //CONSTRUCTORS
    /**
     * @param size: size of the array to be created
     * creates the new bit vector and fills it with 0's by calling the other constructor below
     */
    public BitVector(int size) {
        this(size,false);
    }
   
    
    /**
     * @param size: size of the array to be created
     * @param initVal: fills the array with either only with ones(case true) or zeros(case false)
     * throws illegal argument exception in case of invalid size
     */
    public BitVector(int size , boolean initVal) {
        checkArgument(size % Integer.SIZE == 0 && size > 0);
        bits = new int[size / Integer.SIZE];
        if(initVal) {
            Arrays.fill(bits, ALL_ONES);
        }   
    }
    
    /**
     * helper constructor
     * @param arr: integer array to directly pass the field bits
     */
    private BitVector(int[] arr) {
        Objects.requireNonNull(arr);
        bits = arr; 
    }
    
       
    //METHODS
    
    /**
     * @return bits' length multiplied by Integer.SIZE because 
     * each element contains 32(Integer.SIZE) bits 
     */
    public int size() {
        return bits.length * Integer.SIZE;
    }
    
    /**
     * @param index : index of bit we want to test, elements of array 
     * are considered particular bit elements for parameter index
     * (each array element has 32 indexed bits)
     * by division we first get the index of array(outerIndex)
     * then we get the index of the bit(innerIndex)
     * @return test method of Bits
     */
    public boolean testBit(int index) {
        checkArgument(index >= 0 && index < bits.length * Integer.SIZE);
        int outerIndex = index / Integer.SIZE;
        int innerIndex = index % Integer.SIZE;
        return Bits.test(bits[outerIndex], innerIndex) ;
    }

    
    /**
     * @return complement of each element of bits
     * (thus complement of every other bit)
     * contained in a new BitVector
     */
    public BitVector not() {
        int[] complementArray = new int[bits.length];
        for(int i = 0 ; i <bits.length ; ++i ) {
            complementArray[i] = ~bits[i];
        }
        return new BitVector(complementArray);
    }
    
    /**
     * first checks if they have the same length
     * @param that : other BitVector we want to compare
     * @return conjunction of every element  
     * contained in a new BitVector
     */
    public BitVector and(BitVector that) {
        checkArgument(this.bits.length == that.bits.length);
        int[] conjunction = new int[bits.length];
        for(int i=0; i < bits.length; ++i) {
            conjunction[i] = this.bits[i] & that.bits[i];  
        }
        return new BitVector(conjunction);
    }
    
    /**
     * first check if they have the same length
     * @param that : other BitVector we want to compare
     * @return disjunction of every element 
     * contained in a new BitVector
     */
    public BitVector or(BitVector that) {
        checkArgument(this.bits.length == that.bits.length);
        int[] disjunction = new int[bits.length];
        for(int i=0; i < bits.length; ++i) {
            disjunction[i] = this.bits[i] | that.bits[i];  
        }
        return new BitVector(disjunction);
    }
    
    
    /**
     * @param startIndex: starting index of the extension
     * @param size: length of the extension
     * @return: zero extended version of the BitVector
     */
    public BitVector extractZeroExtended(int startIndex, int size){
        return extract(startIndex, size , ExtractType.ZERO_EXTENDED);
    }
    
    /**
     * @param startIndex: starting index of the extension
     * @param size: length of the extension
     * @return: wrapped extended version of the BitVector
     */
    public BitVector extractWrapped(int startIndex, int size){
        return extract(startIndex, size , ExtractType.WRAPPED);
    }
    
    
    /**
     * @param startIndex: the beginning of the extract
     * @param size: the length of the extract 
     * @param type: indicates the type of extract
     * @return: the extracted new BitVector object.
     */
    private BitVector extract(int startIndex, int size, ExtractType type) {
        //Avoids invalid array length
        checkArgument(size % Integer.SIZE == 0 && size > 0);
        int[] extractedArray = new int[size / Integer.SIZE];
        int index = startIndex;
        for( int i = 0 ; i < extractedArray.length ; ++i) {
            //Calls the helper method that evaluates the first 32 bits element start from the index
            extractedArray[i] =  startElement(index, type); 
            index += Integer.SIZE;
        }
        return new BitVector(extractedArray);
    }
    
    
    /**
     * @param startIndex: the beginning of the bits 
     * @param type: indicates the type of extract
     * @return : the first element (32 bits integer value) of the extract planning to do
     */
    private int startElement(int startIndex, ExtractType type) {
        int mod = Math.floorMod(startIndex, Integer.SIZE);
        int ratio = Math.floorDiv( startIndex , Integer.SIZE);
        int index = Math.floorMod( ratio, bits.length);
        if (mod == 0) {
            if (type == ExtractType.ZERO_EXTENDED && (startIndex < 0 || startIndex >= bits.length * Integer.SIZE))
                return 0;
            return bits[index];
        }
        //Represents two different integer values to be merged according to rule
        int left;
        int right;
        if(type == ExtractType.WRAPPED) {
            right = bits[index];
            left = bits[Math.floorMod(index + 1 , bits.length)];
        }
        
        else {   
            int secondIndex = startIndex + Integer.SIZE;
            int secondRatio = Math.floorDiv( secondIndex , Integer.SIZE);
            right = ratio >= 0 && ratio < bits.length ?  bits[ratio] : 0;
            left = secondRatio >= 0 && secondRatio < bits.length ?  bits[secondRatio] : 0;
        }        
        
        //temporary value to extract the index
        int bitExtractor = Bits.mask(mod) - 1;
        
        //Merging the two integers in appropriate form
        left = left & bitExtractor;
        left = left << Integer.SIZE - mod;
        right = right & ~bitExtractor;
        right = right >>> mod;
        
        return  right | left;  
    }
    
    
    /**
     * @param distance: the distance of shift planned to do
     * @return: shifted bits by calling the extractZeroExtended with trivial start index and distance
     */
    public BitVector shift(int distance) {
        return extractZeroExtended(-distance , size());
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     * returns the hash value of the object considering the structure of its array
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(bits);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     * Compares the hash codes of two bit vector objects
     */
    @Override
    public boolean equals(Object that) {
        return this.hashCode() == that.hashCode();
    }
    
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     * returns concatenated integer values in the binary string form 
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String binaryString;
        int leadingZeros;
        for(int i = bits.length - 1; i >= 0 ; --i) {
            leadingZeros = Integer.numberOfLeadingZeros(bits[i]);
            //considers the leading zeros which is skipped by Integer.toBinaryString()
            for(int j = 0 ; j < leadingZeros; ++j) {
                sb.append('0');
            }
            //Avoids putting an extra zero in the case where leadingZeros = 32 (case 0)
            if(bits[i] != 0) {
                //Appends the binary string value to the string to be returned
                binaryString = Integer.toBinaryString(bits[i]);
                sb.append(binaryString);
            }
        }
        return sb.toString();
    }
    
    
    //BUILDER INNER CLASS
    public static final class Builder{
        
        private final int QUOTIENT = Integer.SIZE / Byte.SIZE;
        
        private boolean alreadyBuilt;
        private int[] bitArray;
        
        /**
         * @param size: size of the BitVector to be builded
         */
        public Builder(int size) {
            checkArgument(size % Integer.SIZE == 0 && size > 0);
            bitArray = new int[size / Integer.SIZE];
            alreadyBuilt = false;
        }
        
        /**
         * @param index: index of bytes to put
         * @param number: represents bytes to put in te index
         * @return: the builder itself(this), throws IllegalStateException if build() is already called
         */
        public Builder setByte(int index , int number) {
            checkBits8(number);
            if(alreadyBuilt) {
                throw new IllegalStateException();
            }
            
            if(index < 0 || index  >= bitArray.length * Integer.SIZE ) {
                throw new IndexOutOfBoundsException();
            }
                               
            int arrayIndex = index / QUOTIENT;
            int byteIndex = index % QUOTIENT;
            
            //shifting the number to appropriate index 
            number = number << Byte.SIZE * byteIndex;
            
            //temporary variable stores 0's in the index (where we'll put the number), 1's everywhere else
            int onesEverywhere = Bits.mask((Byte.SIZE * byteIndex) + 1);
            --onesEverywhere;
            
            //evacuates the index to put our new value(number)
            bitArray[arrayIndex] =  bitArray[arrayIndex] & onesEverywhere;
            
            //finally inserts the number to the index
            bitArray[arrayIndex] = bitArray[arrayIndex] | number;
            return this;
        }
        
        /**
         * @return: the BitVector object wanted to be created, throws IllegalStateException 
         * if the method is already called  once.        
         */
        public BitVector build(){
            if(alreadyBuilt) {
                throw new IllegalStateException();
            }
            alreadyBuilt = true;
            return new BitVector(bitArray);
        }
    }
}