package ch.epfl.gameboj;

public interface Register {
    
    abstract int ordinal();
    
    public default int index() {
        return this.ordinal();
    }
}
