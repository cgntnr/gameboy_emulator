package ch.epfl.gameboj.bits;

import static ch.epfl.gameboj.Preconditions.checkArgument;
import static ch.epfl.gameboj.Preconditions.checkBits8;

import java.util.Objects;

public final class Bits {
  
    private final static int[] arr = new int[] { 0x00, 0x80, 0x40, 0xC0, 0x20, 0xA0, 0x60, 0xE0,
            0x10, 0x90, 0x50, 0xD0, 0x30, 0xB0, 0x70, 0xF0, 0x08, 0x88,
            0x48, 0xC8, 0x28, 0xA8, 0x68, 0xE8, 0x18, 0x98, 0x58, 0xD8,
            0x38, 0xB8, 0x78, 0xF8, 0x04, 0x84, 0x44, 0xC4, 0x24, 0xA4,
            0x64, 0xE4, 0x14, 0x94, 0x54, 0xD4, 0x34, 0xB4, 0x74, 0xF4,
            0x0C, 0x8C, 0x4C, 0xCC, 0x2C, 0xAC, 0x6C, 0xEC, 0x1C, 0x9C,
            0x5C, 0xDC, 0x3C, 0xBC, 0x7C, 0xFC, 0x02, 0x82, 0x42, 0xC2,
            0x22, 0xA2, 0x62, 0xE2, 0x12, 0x92, 0x52, 0xD2, 0x32, 0xB2,
            0x72, 0xF2, 0x0A, 0x8A, 0x4A, 0xCA, 0x2A, 0xAA, 0x6A, 0xEA,
            0x1A, 0x9A, 0x5A, 0xDA, 0x3A, 0xBA, 0x7A, 0xFA, 0x06, 0x86,
            0x46, 0xC6, 0x26, 0xA6, 0x66, 0xE6, 0x16, 0x96, 0x56, 0xD6,
            0x36, 0xB6, 0x76, 0xF6, 0x0E, 0x8E, 0x4E, 0xCE, 0x2E, 0xAE,
            0x6E, 0xEE, 0x1E, 0x9E, 0x5E, 0xDE, 0x3E, 0xBE, 0x7E, 0xFE,
            0x01, 0x81, 0x41, 0xC1, 0x21, 0xA1, 0x61, 0xE1, 0x11, 0x91,
            0x51, 0xD1, 0x31, 0xB1, 0x71, 0xF1, 0x09, 0x89, 0x49, 0xC9,
            0x29, 0xA9, 0x69, 0xE9, 0x19, 0x99, 0x59, 0xD9, 0x39, 0xB9,
            0x79, 0xF9, 0x05, 0x85, 0x45, 0xC5, 0x25, 0xA5, 0x65, 0xE5,
            0x15, 0x95, 0x55, 0xD5, 0x35, 0xB5, 0x75, 0xF5, 0x0D, 0x8D,
            0x4D, 0xCD, 0x2D, 0xAD, 0x6D, 0xED, 0x1D, 0x9D, 0x5D, 0xDD,
            0x3D, 0xBD, 0x7D, 0xFD, 0x03, 0x83, 0x43, 0xC3, 0x23, 0xA3,
            0x63, 0xE3, 0x13, 0x93, 0x53, 0xD3, 0x33, 0xB3, 0x73, 0xF3,
            0x0B, 0x8B, 0x4B, 0xCB, 0x2B, 0xAB, 0x6B, 0xEB, 0x1B, 0x9B,
            0x5B, 0xDB, 0x3B, 0xBB, 0x7B, 0xFB, 0x07, 0x87, 0x47, 0xC7,
            0x27, 0xA7, 0x67, 0xE7, 0x17, 0x97, 0x57, 0xD7, 0x37, 0xB7,
            0x77, 0xF7, 0x0F, 0x8F, 0x4F, 0xCF, 0x2F, 0xAF, 0x6F, 0xEF,
            0x1F, 0x9F, 0x5F, 0xDF, 0x3F, 0xBF, 0x7F, 0xFF, };
  
    private Bits() {}

    /**
     * @param index
     * @return the wanted index as one and others as zero
     */
    public static int mask(int index) {

        index = Objects.checkIndex(index, Integer.SIZE);

        return 1 << index;
    }
    
    /**
     * @param bits from which we get the index
     * @param index to test
     * @return true if the checked index is 1 by controlling
     * the conjunction of bits and masked index, thus comparing 1 and
     * corresponding bit of bits
     */
    public static boolean test(int bits, int index) {

        index = Objects.checkIndex(index, Integer.SIZE);
        return (mask(index) & bits) == mask(index);

    }

    /**
     * @param bits from which we get the index
     * @param bit that we look for
     * @return the same result by a call to the test method above but also acquires the index of that bit
     */
    public static boolean test(int bits, Bit bit) {
        return test(bits, bit.index());

    }

    /**
     * @param bits for which we will set the index
     * @param index to know which bit to set
     * @param newValue : wanted value of indexed bit
     * @return bits after setting 
     */
    public static int set(int bits, int index, boolean newValue) {
        index = Objects.checkIndex(index, Integer.SIZE);

        int temp = 1;
        temp = temp << index;

        //since a disjunction between 0 and a bit won't change the bit, this part will only 
        //set the indexed bit as 1 
        if (newValue) {
            return bits | temp;
        }
        //since a conjunction between 1's and a bit won't change the bit, this part will only
        //set the indexed bit as 0
        return bits & ~temp;

    }

    /**
     * @param size : number of 'bit's we want to clip 
     * @param bits : value we want to clip 
     * @return clipped size of bits beginning from the least significant bit
     */
    public static int clip(int size, int bits) {
        checkArgument(size >= 0 && size <= Integer.SIZE);

        int sum = 0;
        // by repetitive calls to mask computes wanted digits and adds them up
        for (int i = 0; i < size; ++i) {
            sum = sum + mask(i);
        }
        return bits & sum;
    }
    
    /**
     * @param bits : value from which we want to extract bits
     * @param start : start index of extracted part
     * @param size : number of 'bit's to extract
     * @return the wanted size of bits starting by argument start with same logic as clip
     */
    public static int extract(int bits, int start, int size) {
        start = Objects.checkFromIndexSize(start, size, Integer.SIZE);
        bits = bits >> start;
        int temp = 0;
        for (int i = 0; i < size; ++i) {
            temp = temp + mask(i);
        }
        return temp & bits;
    }

    /**
     * @param size : number of bits we want to rotate
     * @param bits : bits in which we do rotating
     * @param distance
     * @return the rotated form of the least significant "size" bits of argument bits for wanted distance
     */
    public static int rotate(int size, int bits, int distance) {
        checkArgument(size > 0 && size <= Integer.SIZE);

        distance = Math.floorMod(distance, size);
        int tempBits = clip(size, bits << distance);

        return bits >>> (size - distance) | tempBits;
    }

    
    /**
     * @param b : value we want to interpret as signed
     * @return signed value by first casting to byte to truncate and then casting back 
     * to int in order to add 0's for 8-32 indexed bits 
     */
    public static int signExtend8(int b) {
        b = checkBits8(b);
        byte truncatedValue = (byte) b;
        int extendedValue = (int) truncatedValue;
        return extendedValue;
    }

    /**
     * @param b : 8 bit value that we want to reverse
     * @return reversed version of b arguments by taking advantage of given array
     */
    public static int reverse8(int b) {
        b = checkBits8(b);
            return arr[b];
     
    }

    /**
     * @param b : 8 bits value (controlled by Preconditions)
     * @return the complement of argument by taking an exclusive or with 0b11111111
     */
    public static int complement8(int b) {
        b = checkBits8(b);
        int temp = 0b11111111;
        return b ^ temp;
    }

    /**
     * @param highB : 8 most significant bits 
     * @param lowB : 8 least significant bits
     * both highB and lowB are controlled by Preconditions
     * @return the chained value in correct order
     */
    public static int make16(int highB, int lowB) {
        highB = checkBits8(highB);
        lowB = checkBits8(lowB);
        return (highB << 8) + lowB;
    }

}