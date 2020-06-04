package ch.epfl.gameboj.component.cartridge;

import static ch.epfl.gameboj.Preconditions.checkBits16;

import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Rom;


public final class MBC0 implements Component {

   private Rom rom;
   private final static int ROM_FILE_SIZE = 32768; 
    
    public MBC0(Rom rom) {
        Objects.requireNonNull(rom);
        Preconditions.checkArgument(rom.size() == ROM_FILE_SIZE);

        this.rom = rom;

    }
    

    //reads the value on address by call to read method of Rom(controls first if address is in range)
    @Override
    public int read(int address) {
        address = checkBits16(address);
        if (address >= 0 && address < ROM_FILE_SIZE) {
            return rom.read(address);
        }
        return NO_DATA;
    }

    // can't write on Rom so it simply returns 
    @Override
    public void write(int address, int data) {

    }

}
