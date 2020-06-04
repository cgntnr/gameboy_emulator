package ch.epfl.gameboj.component;

import static ch.epfl.gameboj.Preconditions.checkBits16;
import static ch.epfl.gameboj.Preconditions.checkBits8;
import static java.util.Objects.requireNonNull;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;

public final class Joypad implements Component {

    //enumeration Key represents the GameBoy keys
    //they are sorted such a way that first 2 bits of their ordinal 
    //represents the column index and the last bit(lsb) represents the line index
    public enum Key {
        RIGHT, A, LEFT, B, UP, SELECT, DOWN, START
    }

    //CONSTANTS
    private static final int UNWRITABLE_BIT_COUNT = 4;
    private static final int MSB_2_BITS_FIXER = 0b0011_1111;
    
    // FIELDS
    private final Cpu cpu;
    private int p1;
    private int line0;
    private int line1;

    // CONSTRUCTORS
    /**
     * @param cpu: the cpu of the GameBoy
     * throws NullPointerException when cpu is null 
     */
    public Joypad(Cpu cpu) {
        requireNonNull(cpu);
        this.cpu = cpu;
    }

    // METHODS
    
    /* (non-Javadoc)
     * @see ch.epfl.gameboj.component.Component#read(int)
     * validating that the address is 16 bits
     * updates the p1 according to the situation of the lines
     * the case address is valid returns  the complementary of p1
     * otherwise returns NO_DATA as expected 
     */
    @Override
    public int read(int address) {
        checkBits16(address);
        if (Bits.test(p1, 4)) {
            p1 = p1 | line0;
        }
        
        if (Bits.test(p1, 5)) {
            p1 = p1 | line1;
        }

        if (address == AddressMap.REG_P1) {          
            return Bits.complement8(p1);
        }
        return NO_DATA;
    }

    /* (non-Javadoc)
     * @see ch.epfl.gameboj.component.Component#write(int, int)
     * validating the address and the data
     * complementing the data
     * making sure that the last 4 bits of p1 can not be changed
     * updating the data
     */
    @Override
    public void write(int address, int data) {
        checkBits16(address);
        checkBits8(data);
        if (address == AddressMap.REG_P1) {
            data = Bits.complement8(data);
            data = data >>> UNWRITABLE_BIT_COUNT;
            data = data << UNWRITABLE_BIT_COUNT;
            p1 = data;
            
          //making the 2 msb bits of p1 always zero
          p1 = p1 & MSB_2_BITS_FIXER;
            
        }
    }

    /**
     * @param key: GameBoy key that is pressed
     * updating the appropriate line according to the ordinal 
     * of the key in the enumeration. Request interrupt if necessary
     */
    public void keyPressed(Key key) {
        int index = key.ordinal();
        int columnIndex = Bits.extract(index, 1, 2);
        int bitExtractor = Bits.mask(columnIndex);
        if(Bits.test(index, 0)){
            line1 = line1 | bitExtractor;
        }
        else{
            line0 = line0 | bitExtractor;
        }
        requestInterrupt();
    }
    
    /**
     * @param key: GameBoy key that is released
     * updating the appropriate line according to the ordinal 
     * of the key in the enumeration. Request interrupt if necessary
     */
    public void keyReleased(Key key) {
        int index = key.ordinal();
        int columnIndex = Bits.extract(index, 1, 2);
        int bitExtractor = Bits.mask(columnIndex);
        
        bitExtractor = Bits.complement8(bitExtractor);
        if (Bits.test(index, 0)) {
            line1 = line1 & bitExtractor;
        }

        else {
            line0 = line0 & bitExtractor;
        }
        requestInterrupt();
    }
    
    /**
     * Requests interrupt from the cpu, checking the 4th and
     * 5th bit of p1
     */
    private void requestInterrupt() {
        if (!Bits.test(p1, 4) || !Bits.test(p1, 5)) {
            cpu.requestInterrupt(Interrupt.JOYPAD);
        }
    }

}