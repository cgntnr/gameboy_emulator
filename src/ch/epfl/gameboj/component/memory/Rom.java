package ch.epfl.gameboj.component.memory;
import static ch.epfl.gameboj.Preconditions.checkBits8;
import java.util.Arrays;

public final class Rom { 

    //fields
    private final byte[] data;

    
    /**
     * @param data : byte array
     * checks if data is null, if not constructs a new Rom 
     * with given data byte array as parameter
     */
    public Rom(byte[] data) {
        if (data == null) {
            throw new NullPointerException();
        }

        else {
            this.data = Arrays.copyOf(data, data.length);
        }
    }

   public int size() {
        return data.length; 
    }


    /**
     * @param index we want to read from
     * @return the value on index but since since read deals with 
     * positive values, it uses toUnsignedBit
     */
    public int read(int index) {

        if (index < 0 || index >= data.length) {
            throw new IndexOutOfBoundsException();
        }

        int unsignedData = Byte.toUnsignedInt(data[index]);
        return checkBits8(unsignedData);
    }

}