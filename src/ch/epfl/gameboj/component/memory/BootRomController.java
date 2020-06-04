package ch.epfl.gameboj.component.memory;

import java.util.Objects;

import static ch.epfl.gameboj.Preconditions.checkBits16;
import static ch.epfl.gameboj.Preconditions.checkBits8;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cartridge.Cartridge;

public final class BootRomController implements Component {

    private boolean bootRomDisabled = false;
    private Cartridge cartridge;
   
    //constructor
    public BootRomController(Cartridge cartridge) {
        Objects.requireNonNull(cartridge);
       
        this.cartridge = cartridge;
        
    }

    
    /* (non-Javadoc)
     * @see ch.epfl.gameboj.component.Component#read(int)
     */
    //if address is in limits of bootRom and the bootRom isn't disabled,
    //returns the corresponding data on address in bootRom
    //else reads from cartridge
    @Override
    public int read(int address) {
        address = checkBits16(address);
        
        if (address >= AddressMap.BOOT_ROM_START && address < AddressMap.BOOT_ROM_END) {
            if (!bootRomDisabled) {
                return Byte.toUnsignedInt(BootRom.DATA[address]);
            }
              
        }
            return cartridge.read(address);

    }

    /* (non-Javadoc)
     * @see ch.epfl.gameboj.component.Component#write(int, int)
     */
    //By writing on address AddressMap.REG_BOOT_ROM_DISABLE, we signal that bootRom is disabled,
    //so in that case, even the address is within limits of bootRom, read of cartridge will be called
    //else writes data on address 
    @Override
    public void write(int address, int data) {
        address = checkBits16(address);
        data = checkBits8(data);
        
        if(address == AddressMap.REG_BOOT_ROM_DISABLE) {
            bootRomDisabled = true;
            
        }else 
            cartridge.write(address, data);
        
        
    }

}
