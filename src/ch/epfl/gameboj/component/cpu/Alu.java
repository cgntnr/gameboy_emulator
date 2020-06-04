package ch.epfl.gameboj.component.cpu;

import static ch.epfl.gameboj.Preconditions.checkBits16;
import static ch.epfl.gameboj.Preconditions.checkBits8;
import static ch.epfl.gameboj.bits.Bits.clip;
import static ch.epfl.gameboj.bits.Bits.extract;
import static ch.epfl.gameboj.bits.Bits.mask;
import static ch.epfl.gameboj.bits.Bits.test;

import java.util.Objects;

import ch.epfl.gameboj.bits.Bit;
public final class Alu {
   
    //fields
    private final static int GREATEST_8_BITS_VALUE = 0xFF;
    private final static int GREATEST_4_BITS_VALUE = 0xF;
    private final static int SMALLEST_9_BITS_VALUE = 0x100;
    
    public enum Flag implements Bit {
        UNUSED_0, UNUSED_1, UNUSED_2, UNUSED_3, C, H, N, Z
    }

    public enum RotDir {
        LEFT, RIGHT
    }
    
    /**
     * @param z : value from the Flag enum indicates whether the result is 0 or not 
     * @param n : value from the Flag enum indicates whether the operation is a subtraction or not  
     * @param h : value from the Flag enum indicates whether the result produces an half carry or not 
     * @param c : value from the Flag enum indicates whether the result produces a carry or not 
     * @return : mask value from the flags 
     */
    public static int maskZNHC(boolean z, boolean n, boolean h, boolean c) {

        int maskedValue = 0;
        if (c) {
            maskedValue +=  Flag.C.mask();
        }

        if (h) {
            maskedValue +=  Flag.H.mask();
        }

        if (n) {
            maskedValue +=  Flag.N.mask();
        }

        if (z) {
            maskedValue +=  Flag.Z.mask();
        }
        return  maskedValue;

    }
    
    // packs the flags and value all together
    private static int packValueZNHC(int v, boolean z, boolean n, boolean h, boolean c) {
        
        return (v << 8) + maskZNHC(z, n, h, c);
    }

    // takes out the value out of valueFlags
    /**
     * @param valueFlags : package of value/flags
     * @return: the value from the package of value/flags
     */
    public static int unpackValue(int valueFlags) {
        int mostSignificantBit = Integer.highestOneBit(valueFlags);
        // if mostSignificantBit is smaller than 7 , it means the value is 0 
        if (mostSignificantBit <= 7) {
            return 0;
        }
        // if mostSignificantBit is smaller equal to 15 , it means the value is 8 bits so we extract 8 bits starting from index 8
        if (mostSignificantBit <= 15) {
            return extract(valueFlags, 8, 8);
        }
        // else , it means the value is more that 8 bits so we extract 16 bits starting from index 8
        return extract(valueFlags, 8, 16);
    }

    // extracts the first 8 bits of valueFlags which indicates flags
    /**
     * @param valueFlags : package of value/flags
     * @return : the flags from the package of value/flags
     */
    public static int unpackFlags(int valueFlags) {
        return extract(valueFlags, 0, 8);
    }

    
    /**
     * @param l : first argument of the addition, 8 bits value
     * @param r : second argument of the addition, 8 bits value
     * @param c0: boolean represents the carry value from the previous calculations
     * @return : adds the arguments considering the c0 and returns packed value/flags associated 
     *           with the operation
     */
    public static int add(int l, int r, boolean c0) {
        l = checkBits8(l);
        r = checkBits8(r);
        
        boolean h;
        boolean c;
        int result = l + r;
        
        //Extracting last 4 bits to calculate H flag
        int nonSignificantL = extract(l, 0, 4);
        int nonSignificantR = extract(r, 0, 4);
        
        //Checking the carry bits
        if(c0) {
            ++nonSignificantL;
            ++result;
        }
        
        h = (nonSignificantL + nonSignificantR > GREATEST_4_BITS_VALUE );
        c = result > GREATEST_8_BITS_VALUE;
        
        //To avoid exceeding the 8 bits
        if (c) {
            result = result - SMALLEST_9_BITS_VALUE;
        }
        
        return packValueZNHC(result, result == 0 ,false , h ,c);
    }

    
    // does the same thing with add method above and a carry as 0 
    /**
     * @param l : first argument of the addition, 8 bits value
     * @param r : second argument of the addition, 8 bits value
     * @return : adds the arguments assuming there is no carry from the previous calculations
     */
    public static int add(int l, int r) {
        return add(l, r, false);
    }

    /**
     * @param l : first argument of the addition, 16 bits value  
     * @param r : second argument of the addition, 16 bits value
     * @return : adds two 16 bits integers and returns the packed value with 
     *           the flags of the least significant 8 bits of the result 
     */
    public static int add16L(int l , int r) {
        l = checkBits16(l);
        r = checkBits16(r);
        
        boolean h ;
        boolean c ;
        
        //separating most and least significant 8 bits of l parameter
        int significantL = extract(l, 8, 8) ;
        int noNsignificantL = extract(l, 0, 8) ;
        
        //separating most and least significant 8 bits of r parameter
        int significantR = extract(r, 8, 8) ;
        int noNsignificantR = extract(r, 0, 8) ;
        
        // summing the non-significant bits 
        int sum1 = add(noNsignificantL,  noNsignificantR);
        int value1 = unpackValue(sum1);
        int flags = unpackFlags(sum1);
        
        h = test(flags ,5); // H flag of the result
        c = test(flags, 4); //C flag of the result
        
        // summing the significant bits 
        int sum2 = add(significantL, significantR ,c);
        int value2 = unpackValue(sum2);
        
        int result = (value2 << 8) + value1;// concatenating the results 
        return packValueZNHC(result,false,false ,h ,c); // returning the result in appropriate form
        
    }

    
    /**
     * @param l : first argument of the addition, 16 bits value  
     * @param r : second argument of the addition, 16 bits value
     * @return : adds two 16 bits integers and returns the packed value with 
     *           the flags of the most significant 8 bits of the result 
     */
    public static int add16H(int l , int r) {
        l = checkBits16(l);
        r = checkBits16(r);
        
        boolean h ;
        boolean c ;
        boolean c0;
        
        //separating most and least significant 8 bits of l parameter
        int significantL = extract(l, 8, 8) ;
        int noNsignificantL = extract(l, 0, 8) ;
        
        //separating most and least significant 8 bits of r parameter
        int significantR = extract(r, 8, 8);
        int noNsignificantR = extract(r, 0, 8);
        
        // summing the non-significant bits 
        int sum1 = add(noNsignificantL,  noNsignificantR);
        int value1 = unpackValue(sum1);
        int flags1 = unpackFlags(sum1);
        c0 = test(flags1, 4);// Carry value to be used in the significant-bit sum
        
        // summing the significant bits
        int sum2 = add(significantL, significantR ,c0);
        int value2 = unpackValue(sum2);
        int flags2 = unpackFlags(sum2);
        c = test(flags2, 4);
        h = test(flags2, 5); 
        
        int result = (value2 << 8) + value1; // concatenating the results 
        return packValueZNHC(result,false,false ,h ,c); // returning the result in appropriate form
    }

    /**
     * @param l : minuend of the subtraction, 8 bits value
     * @param r : subtrahend of the subtraction, 8 bits value
     * @param b0: boolean represents the borrow from the previous calculations
     * @return : calculates the difference considering the b0 and returns packed value/flags associated 
     *           with the operation
     */
    public static int sub(int l, int r ,boolean b0) {
        //Very similar process with add method
        l = checkBits8(l);
        r = checkBits8(r);
        
        boolean z;
        boolean h;
        boolean c;
        int result = l - r;
        
        int nonSignificantL = extract(l, 0, 4);
        int nonSignificantR = extract(r, 0, 4);
        if(b0) {
            --nonSignificantL;
            --result;
        }
        
        h = (nonSignificantL - nonSignificantR < 0 );
        c = result < 0;
        
        if (c) {
            result = result + SMALLEST_9_BITS_VALUE;
        }
        
        z = (result == 0);
        return packValueZNHC(result, z ,true, h ,c);
    }
    /**
     * @param l : minuend of the subtraction, 8 bits value
     * @param r : subtrahend of the subtraction, 8 bits value
     * @return : the difference assuming there is no borrow from the previous calculations
     */
    public static int sub(int l, int r) {
        return sub(l, r, false);
    }
    
    
    /**
     * @param v : 8 bits value
     * @param n : boolean represents N flag
     * @param h : boolean represents H flag
     * @param c : boolean represents C flag
     * @return : transforms the value to DCB format using the 
     * algorithm proposed in instructions          
     */
    public static int bcdAdjust(int v, boolean n, boolean h, boolean c) {
        v = checkBits8(v);
        
        boolean fixL = (h || (!n && (extract(v, 0, 4)) > 0x09));
        boolean fixH = (c || (!n && (v > 0x99)));

        int binaryH = 0;
        int binaryL = 0;

        if (fixH) {
            binaryH = 1;
        }
        if (fixL) {
            binaryL = 1;
        }
        int fix = (0x60 * binaryH) + (0x06 * binaryL);
        int valAdjusted;

        if (n) {
            valAdjusted = v - fix;
        } else {
            valAdjusted = v + fix;
        }

        return packValueZNHC(clip(8,valAdjusted), clip(8,valAdjusted) == 0, n, false, fixH);

    }

    
    /**
     * @param l: 8 bits int value
     * @param r: 8 bits int value
     * @return: does the conjunction of two 8 bit values and turns the packed value with flags , 
     *          checks also if they are 8 bits(the latters is same for all methods)
     */
    public static int and(int l, int r) {
        l = checkBits8(l);
        r = checkBits8(r);
        
        int conjunction = (l & r) << 8;

        return conjunction + maskZNHC(conjunction == 0, false, true, false);
    }
    
    
    /**
     * @param l: 8 bits int value
     * @param r: 8 bits int value
     * @return: does the disjunction of two 8 bit values and turns the packed value with flags
     */
    public static int or(int l, int r) {
        l = checkBits8(l);
        r = checkBits8(r);
        
        int disjunction = (l | r);

        return packValueZNHC(disjunction, disjunction == 0, false, false, false);
    }
    
    /**
     * @param l: 8 bits int value
     * @param r: 8 bits int value
     * @return: does the exclusive disjunction of two 8 bit values and turns the packed value with flags
     */
    public static int xor(int l, int r) {
        l = checkBits8(l);
        r = checkBits8(r);
        
        int exDisjunction = (l ^ r) << 8;

        return exDisjunction + maskZNHC(exDisjunction == 0, false, false, false);
    }

    /**
     * @param v : value we want to shift
     * checks first the ejected bit to determine flag C and then does shifting
     * @return packed value after shifting 
     */
    public static int shiftLeft(int v) {
        v = checkBits8(v);
        boolean c = test(v, 7);

        v = v << 1;
        int extractedValue = extract(v, 0, 8);

        return packValueZNHC(extractedValue, extractedValue == 0, false, false, c);

    }
 
    /**
     * @param v : value to be shifted
     * checks first the ejected bit to determine flag C and then does shifting,
     * to do it arithmetically we check most significant bit first with test method
     * and shift accordingly
     * @return packed value after shifting arithmetically
     */
    public static int shiftRightA(int v) {
        v = checkBits8(v);
        boolean c = test(v,0);  // to determine flag C
      
        if (test(v, 7)) {
            
                         
            v = v >> 1;

            return packValueZNHC(mask(7) + v, v == 0, false, false, c);

        } else {

            v = v >>> 1;
                 
            return packValueZNHC(v, v == 0, false, false, c);

        }
    }
  
    /**
     * @param v : value to be shifted
     * checks first the ejected bit to determine flag C and then does shifting,
     * @return the shifted value with proper flag C in a packed way
     */
    public static int shiftRightL(int v) {
        v = checkBits8(v);
        if (test(v, 0)) {       // to determine flag C

            v = v >>> 1;
            return packValueZNHC(v, v == 0, false, false, true);
        }

        v = v >>> 1;
        return packValueZNHC(v, v == 0, false, false, false);
    }
    
    
    /**
     * @param d : direction
     * @param v : value to be rotated
     * @return rotated form by first diving cases for LEFT and RIGHT 
     * then we check the ejected bit and assign that bit to value's 
     * corresponding side(most significant bit or least significant bit)
     */
    public static int rotate(RotDir d, int v) {
        v = checkBits8(v);
        boolean c;
        // Case LEFT
        if (d.ordinal() == 0) {
            // Checking the case if the most significant bit is 1
            c = test(v, 7);
            if (c) {
                v = v << 1;
                ++v;
                v = v - SMALLEST_9_BITS_VALUE;
            }

            else {
                v = v << 1;
            }
        }

        // Case RIGHT
        else {
            // Checking the case if the least significant bit is 1
            c = test(v, 0);
            if (c) {
                v = v >> 1;
                v = v + mask(7) ; //mask call is to correct most significant bit
            }

            else {
                v = v >> 1;
            }
        }
        return packValueZNHC(v, v == 0, false, false, c);

        
    }

    /**
     * @param d : direction
     * @param v : value to be rotated
     * @param c : flag C 
     * @return rotated packed value
     * First we check parameter c to know form of 9 bit value,
     * then we divide each case for right and left.
     */
    public static int rotate(RotDir d, int v, boolean c) {
        v = checkBits8(v);

        if (c == false) { // case for C is 0

            if (d.ordinal() == 0) { // case for LEFT
                int extracted7Bit = extract(v, 0, 7);
                int rotated = extracted7Bit << 1;

                return packValueZNHC(rotated, rotated == 0, false, false, test(v, 7));

            } else { // case for RIGHT
                int rotated = extract(v, 1, 7);

                return packValueZNHC(rotated, rotated == 0, false, false,
                        test(v, 0));
            }

        } else { // case for C is 1
                
            if (d.ordinal() == 0) { // case for LEFT
                int extracted7Bit = extract(v, 0, 7);
                int rotated = extracted7Bit << 1;
                rotated = rotated + 1;

                return packValueZNHC(rotated, rotated == 0, false, false, test(v, 7));

            } else { // case for RIGHT
                int extracted7Bit = extract(v, 1, 7);
                int rotated = mask(7) + extracted7Bit;

                return packValueZNHC(rotated, rotated == 0, false, false, test(v, 0));
            }

        }

    }
   
    /**
     * @param v : value to be swapped
     * @return swapped version of most significant and least significant four bits 
     * in a packed way
     */
    public static int swap(int v) {
        v = checkBits8(v);

        int lsb4 = extract(v, 0, 4);
        int msb4 = extract(v, 4, 4);

        lsb4 = lsb4 << 4;         //shifting least significant four bits
        int swapped = lsb4 + msb4;

        return packValueZNHC(swapped, swapped == 0, false, false, false);

    }

    /**
     * @param v : value in which we test a bit
     * @param bitIndex : index of the bit we want to test
     * @return 0 as value and flags Z010 for which Z is true if bit on bitIndex is 1 
     * (all of them in a packed way)
     */
    public static int testBit(int v, int bitIndex) {
        v = checkBits8(v);
        Objects.checkIndex(bitIndex, 8);
        
        if (test(v, bitIndex)) {
            return packValueZNHC(0, false, false, true, false);
        }
        return packValueZNHC(0, true, false, true, false);
    }



}