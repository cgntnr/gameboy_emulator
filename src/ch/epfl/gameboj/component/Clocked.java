package ch.epfl.gameboj.component;

public interface Clocked {
    
    
    /**
     * method to override in class implementing Clocked , deals with 
     * cycling, recalling necessary instructions
     * @param cycle 
     */
    abstract void cycle(long cycle);
        
    
}
