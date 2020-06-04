package ch.epfl.gameboj;

import static ch.epfl.gameboj.Preconditions.checkArgument;

import java.util.Objects;

import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.Timer;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.lcd.LcdController;
import ch.epfl.gameboj.component.memory.BootRomController;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;

public final class GameBoy implements AddressMap {
   
    // FIELDS
    private final Bus bus;
    private final Ram workRam;
    private final RamController ramController;
    private final RamController copyRamController;
    private final Cpu cpu;
    private final BootRomController bootRomController;
    private final Timer timer;
    private final LcdController lcdController;
    private final Joypad joypad;
    private long cyclesSimulated;
    
    //CONSTANTS
    public static final long CYCLES_PER_SECOND = 0b00000000_00010000_00000000_00000000;
    public static final double CYCLES_PER_NANOSECOND = CYCLES_PER_SECOND /1_000_000_000.0 ;
    
    // CONSTRUCTORS
    public GameBoy(Cartridge cartridge) {
      
        //Throws NullPointerException if the argument is null
        Objects.requireNonNull(cartridge);

        // instantiates ram
        workRam = new Ram(WORK_RAM_SIZE);

        // instantiates ram controller and gives control to workRam from
        ramController = new RamController(workRam, WORK_RAM_START);

        // instantiates second ram controller and gives control to workRam from
        copyRamController = new RamController(workRam, ECHO_RAM_START, ECHO_RAM_END);

        // instantiates boot rom controller
        bootRomController = new BootRomController(cartridge);
        
        //instantiates bus and cpu
        bus = new Bus();
        cpu = new Cpu();
        
        //instantiates timer,joypad and lcdController with parameter 
        //cpu to give access for requesting interrupts
        timer = new Timer(cpu);
        joypad = new Joypad(cpu);
        lcdController = new LcdController(cpu); 
              


        // attaches components to bus
        bus.attach(ramController);
        bus.attach(copyRamController);
        bus.attach(timer);
        bus.attach(bootRomController);
        cpu.attachTo(bus);  
        lcdController.attachTo(bus);  
        joypad.attachTo(bus);
        
        cyclesSimulated = 0;
    }

    //METHODS
   
    /**
    * @param cycle
    * simulates the game boy from cyclesSimulated to cycles - 1
    * throws IllegalArgumentException in case of the argument is 
    * strictly bigger than the field cyclesSimulated
    */
    public void runUntil(long cycle) {
        checkArgument(cyclesSimulated <= cycle);
        for (long i = cyclesSimulated; i < cycle; ++i) {
            timer.cycle(i);
            lcdController.cycle(i);  
            cpu.cycle(i);
            
        }
        cyclesSimulated = cycle;
    }
   
   //Accessors
    public long cycles() {
        return cyclesSimulated;
    }

    public Timer timer() {
        return timer;
    }

    public Bus bus() {
        return bus;
    }

    public Cpu cpu() {
        return cpu;
    }
    
    public LcdController lcdController() {
        return lcdController;
    }
    public Joypad joypad() {
        return joypad;
    }
}