package ch.epfl.gameboj;

import static ch.epfl.gameboj.Preconditions.checkBits8;

import java.util.Objects;

import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;

public final class RegisterFile <E extends Register> {
    //FIELDS
    private final byte[] collection; 
    
    //CONSTRUCTORS
    public RegisterFile(E[] allRegs) {     
       allRegs = Objects.requireNonNull(allRegs); 
       collection = new byte[allRegs.length];                 
    }
    
    //METHODS
    /**
     * @param reg: register that we are calling from our byte array
     */
    public int get(E reg) {
        return Byte.toUnsignedInt(collection[reg.index()]);   
    }
    
    /**
     * @param reg : register that we are calling from our byte array
     * @param newValue : value to put in the register
     */
    public void set(E reg, int newValue) {
        newValue = checkBits8(newValue);
        collection[reg.index()] = (byte)newValue;
    }
    
    /**
     * @param reg : register that we are calling from our byte array
     * @param b : Bit to test 
     * checks if the given bit of the register is 1 or not
     */
    public boolean testBit(E reg, Bit b) {
        return (collection[reg.index()] & (byte)b.mask()) != 0 ;
    }
    
    
    /**
     * @param reg : register that we are calling from our byte array
     * @param bit : Bit to give the index
     * @param newValue : new value of the mentioned bit of the register 
     * Modifies the value in the given register such that the given bit would be
     * the new value(1 for true and 0 for false)
     */
    public void setBit(E reg, Bit bit, boolean newValue) {
        int index = bit.index();
        int bits = collection[reg.index()];
        collection[reg.index()] = (byte)Bits.set(bits, index, newValue);
    } 
}