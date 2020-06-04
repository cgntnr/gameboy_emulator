package ch.epfl.gameboj.component.cartridge;

import static ch.epfl.gameboj.Preconditions.checkBits16;
import static ch.epfl.gameboj.Preconditions.checkBits8;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Rom;

public final class Cartridge implements Component {

    

    private final static int TYPE_CONTROL_BYTE = 0x147;
    private final static int SIZE_CONTROL_BYTE = 0x149;
    private final static int minNumberForTypeMBC1 = 1;
    private final static int maxNumberForTypeMBC1 = 3;
    private final Component mbc;

    private final static int mbc1RamSizeArray[] = {0, 2048, 8192, 32768} ;  
 
    
    private Cartridge(Component mbc) {   
            this.mbc = mbc;
    }
    
    /**
     * @param romFile : file we get to form Cartridge
     * @return a Cartridge by passing through Rom and (MBC0 or MBC1)
     * type of mbc is determined by the type control byte
     * @throws IOException
     * this method creates a new stream and stocks all the bytes from that stream 
     * in cartridgeArray then passing through right mbc(by checking mbc type) constructs Cartridge
     */
    public static Cartridge ofFile(File romFile) throws IOException {

        // stocks all the bytes of romFile into cartridgeArray then closes stream
        InputStream stream = new FileInputStream(romFile);
        byte[] cartridgeArray;
        cartridgeArray = stream.readAllBytes();                                           
        stream.close();
        
        checkBits8(cartridgeArray[TYPE_CONTROL_BYTE]);
  
        if (cartridgeArray[TYPE_CONTROL_BYTE] == 0) {
            return new Cartridge(new MBC0(new Rom(cartridgeArray)));

        } else if (cartridgeArray[TYPE_CONTROL_BYTE] >= minNumberForTypeMBC1
                && cartridgeArray[TYPE_CONTROL_BYTE] <= maxNumberForTypeMBC1) {
            
            //since some mbc1 types has a ram, their size is determined
            //and then given as a second parameter to the MBC1 constructor
            int ramSize = mbc1RamSizeArray[cartridgeArray[SIZE_CONTROL_BYTE]];
            return new Cartridge(new MBC1(new Rom(cartridgeArray), ramSize));
        }
        
        throw new IllegalArgumentException("Unimplemented MBC Type");
        
    }
 
    /* (non-Javadoc)
     * @see ch.epfl.gameboj.component.Component#read(int)
     * reads data on address by a call to read method of MBC0 or MBC1
     */
    @Override
    public int read(int address) {
       address = checkBits16(address);
        return mbc.read(address);     
    }

      
    /* (non-Javadoc)
     * @see ch.epfl.gameboj.component.Component#write(int, int)
     * writes data on address by a call to write method of MBC0 or MBC1
     */
    @Override
    public void write(int address, int data) {
       address = checkBits16(address);
       data = checkBits8(data);
       mbc.write(address, data);
        
    }

}
