package ch.epfl.gameboj.component;

import ch.epfl.gameboj.Bus;

public interface Component {
    
    // declaring a value out of bound to indicate there's no data 
    public static final int NO_DATA = 0x100;
    

    //reads from address(further explanation on overwritten parts)
    int read(int address);  
    

    // writes data on address (further explanation on overwritten parts)
    void write(int address,int data); 
    
    
    //attaches the component to bus by calling attach method of bus 
    default void attachTo(Bus bus){
        bus.attach(this); 
    }
    
    
    
    
    
    
    
}
