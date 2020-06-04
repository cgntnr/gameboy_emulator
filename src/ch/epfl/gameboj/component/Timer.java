package ch.epfl.gameboj.component;

import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;


public final class Timer implements Component, Clocked {
    //FIELDS
    private int primaryTimer;
    private int tima;   //secondary timer
    private int tac;
    private int tma;
    private Cpu cpu;

    //CONSTRUCTORS
    /**
     * Instantiates the fields and avoids invalid null argument
     */
    public Timer(Cpu cpu) {
        Objects.requireNonNull(cpu);
        this.cpu = cpu;
        primaryTimer = 0;
        tima = 0;
        tac = 0;
        tma = 0;
    }

    /*
     * @see ch.epfl.gameboj.component.Clocked#cycle(long)
     * updates the timer 
     */
    @Override
    public void cycle(long cycle) {
        boolean s0 = state();
        primaryTimer = Bits.clip(16, primaryTimer + 4);   
        incIfChange(s0);
    }

    /**
     * @param address : gives access to the fields using their 
     * corresponding address from AddressMap
     */
    @Override
    public int read(int address) {
        address = Preconditions.checkBits16(address);
        if (address == AddressMap.REG_DIV) {
            return Bits.extract(primaryTimer, 8, 8);
        }
        if (address == AddressMap.REG_TIMA) {
            return tima;
        }
        if (address == AddressMap.REG_TMA) {
            return tma;
        }
        if (address == AddressMap.REG_TAC) {
            return tac;
        }
        return NO_DATA;

    }
    
    /**
     * @param address : 16 bits address stores the data 
     * @param data : 8 bits data to write 
     * sets the fields using their 
     * corresponding address from AddressMap
     */
    @Override
    public void write(int address, int data) {
        address = Preconditions.checkBits16(address);
        data = Preconditions.checkBits8(data);
        if (address == AddressMap.REG_DIV) {
            boolean s0 = state();
            primaryTimer = 0;
            incIfChange(s0);
        }
        if (address == AddressMap.REG_TIMA) {

            tima = data;

        }
        if (address == AddressMap.REG_TMA) {
            tma = data;
        }
        if (address == AddressMap.REG_TAC) {
            boolean s0 = state(); 
            tac = data;
            incIfChange(s0);
        }

    }

    /**
     * Helper method indicates the current state of the timer
     * @returns the conjunction of the second bit of tac 
     * and the appropriate index(see table in the instructions) of the primary timer
     */
    private boolean state() {
       int index = 0;
       int indexChooser = Bits.clip(2, tac);       
       if(indexChooser == 0) {
           index = 9;
       }
       else {
           index = 2 * indexChooser + 1; 
       }
       return Bits.test(primaryTimer, index) && Bits.test(tac, 2);
    }
    
    /**
     * @param previousState : boolean represents the previous state of the timer
     * @return(void): arranges the secondary timer(tima) according to previous and current
     * states, request interrupts in case of 0xFF
     */
    private void incIfChange(boolean previousState) {
        if (previousState && !state()) {
            if (tima == 0xFF) {
                cpu.requestInterrupt(Interrupt.TIMER);
                tima = tma;
            } else {
                ++tima;

            }
        }

    }

}
