package ch.epfl.gameboj;

import static ch.epfl.gameboj.Preconditions.checkBits16;
import static ch.epfl.gameboj.Preconditions.checkBits8;

import java.util.ArrayList;
import java.util.Objects;

import ch.epfl.gameboj.component.Component;

public final class Bus {

    
    private final ArrayList<Component> componentList = new ArrayList<Component>();

    
    /**
     * attaches component to bus by adding it to our componentList
     * @param component : what we want to attach
     */
    public void attach(Component component) {
        component = Objects.requireNonNull(component);
        componentList.add(component);
    }

    /**
     * 
     * @param address to read on (first checked if it is 16 bits)
     * throws IndexOutOfBoundsException if not compatible
     * @return the value on address if one of the components attached to bus has a value on it 
     * if the list is all NO_DATA, index will be equal to size of componentList, thus returning default 0xFF value
     */
    public int read(int address) {

        address = checkBits16(address);
        int index = 0;
        while (index < componentList.size() && componentList.get(index).read(address) == Component.NO_DATA) {
            index++;
        }

        if (index == componentList.size()) {
            return 0xFF;
        }

        return componentList.get(index).read(address);
    }

    /**
     * writes the given data to the attached components of bus
     * @param address to be written on (first checked if it is 16 bits)
     * @param data to be written (first checked if it is 8 bits) 
     * for both parameters throws IndexOutOfBoundsException if not compatible
     */
    public void write(int address, int data) {

        address = checkBits16(address);
        data = checkBits8(data);
        for (Component c : componentList) {
            c.write(address, data);
        }
    }
}