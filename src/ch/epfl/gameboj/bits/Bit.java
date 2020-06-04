package ch.epfl.gameboj.bits;

public interface Bit {

    abstract int ordinal();

    /**
     * @returns index , method for clarifying usage of ordinal
     */
    default int index(){
        return ordinal();      
    }
    
    /**
     * @calls the static method from Bits class to apply mask method to a Bit
     */
    default int mask(){
        return Bits.mask(this.index());    
    }

}



