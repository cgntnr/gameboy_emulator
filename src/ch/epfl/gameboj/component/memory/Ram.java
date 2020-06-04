package ch.epfl.gameboj.component.memory;

import static ch.epfl.gameboj.Preconditions.checkArgument;
import static ch.epfl.gameboj.Preconditions.checkBits8;

public final class Ram {
    
    //fields
    private final byte[] data; 

    /**
     * @param size
     * constructs ram of wanted size as a byte array
     */
    public Ram(int size){
        checkArgument(size >= 0);
        data = new byte[size];
    }
    

    public int size(){
        return data.length;
    }
   
    /**
     * @param index
     * if index is not in range throws IndexOutOfBoundsException or 
     * returns the unsigned version of 8 bits value on that index(of data)
     */
    public int read(int index){
        if(index < 0 || index >= data.length ){                
            throw new IndexOutOfBoundsException();
        }
        int temp = Byte.toUnsignedInt(data[index]);
        return checkBits8(temp);
        
    }

    /**
     * @param index
     * @param value : value we want to assign to index of data
     * if index is not in range throws IndexOutOfBoundsException or 
     * writing value to the index(of data) after checking the value is indeed 8 bits
     */
    public void write(int index, int value){
        if(index < 0 || index >= data.length ){
            throw new IndexOutOfBoundsException();
        }
        
        value = checkBits8(value);
        data[index] = (byte)value;
        
    }
    
}