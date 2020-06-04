package ch.epfl.gameboj.component.memory;
import ch.epfl.gameboj.component.Component;
import static ch.epfl.gameboj.Preconditions.checkBits16;

import java.util.Objects;

import static ch.epfl.gameboj.Preconditions.checkArgument;

public final class RamController implements Component{

    //fields 
    private final Ram ram;
    private final int startAddress;
    private final int endAddress;
    

    /**
     * @param ram : controlled ram
     * @param startAddress
     * @param endAddress
     * constructs ramController with access over range between given parameters
     */
    public RamController(Ram ram, int startAddress, int endAddress) {

        // necessary preconditions
        Objects.requireNonNull(ram);

        startAddress = checkBits16(startAddress);
        endAddress = checkBits16(endAddress);
        checkArgument(startAddress < endAddress && (endAddress - startAddress <= ram.size())); 
        //here we check if range is smaller equal to size

        this.ram = ram;
        this.startAddress = startAddress;
        this.endAddress = endAddress;

    }
    
    /**
     * @param ram 
     * @param startAddress : index from which we start to give access
     * constructs ramController with access to whole ram 
     */
    public RamController(Ram ram, int startAddress) {
        this(ram, startAddress, ram.size() + startAddress);

    }

    /* (non-Javadoc)
     * @see ch.epfl.gameboj.component.Component#read(int)
     */
    //if address is in range, reads from index 
    //else returns NO_DATA 
    @Override
    public int read(int address) {

        checkBits16(address);
        if (address >= startAddress && address < endAddress) {
            int index = address - startAddress;
            return ram.read(index);
        }
        return NO_DATA;

    }

    
    /* (non-Javadoc)
     * @see ch.epfl.gameboj.component.Component#write(int, int)
     */
    //if address is in range, writes data on index 
    //if not simply returns 
    @Override
    public void write(int address, int data) {

        checkBits16(address);

        if (address >= startAddress && address < endAddress) {
            int index = address - startAddress;
            ram.write(index, data);
        }

    }

}
