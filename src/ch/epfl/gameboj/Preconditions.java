package ch.epfl.gameboj;

public interface Preconditions {
    
  
    /**
     * checks if the condition is true ,
     * throws IllegalArgumentException if false
     * @param condition
     */
    static void checkArgument(boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * @param value
     * @return the value if it is indeed 8 bits
     * throws IllegalArgumentException otherwise
     */
    static int checkBits8(int value) {
       checkArgument(value >= 0 && value <= 0xFF);
           return value;

    }

    /**
     * @param value
     * @return the value if it is indeed 16 bits
     * throws IllegalArgumentException otherwise
     */
    static int checkBits16(int value) {
        checkArgument(value >= 0 && value <= 0xFFFF);
        return value;

    }
}