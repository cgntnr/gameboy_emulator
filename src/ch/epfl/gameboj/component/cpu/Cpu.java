package ch.epfl.gameboj.component.cpu;

import static ch.epfl.gameboj.Preconditions.checkBits16;
import static ch.epfl.gameboj.Preconditions.checkBits8;
import static ch.epfl.gameboj.bits.Bits.clip;
import static ch.epfl.gameboj.bits.Bits.complement8;
import static ch.epfl.gameboj.bits.Bits.extract;
import static ch.epfl.gameboj.bits.Bits.make16;
import static ch.epfl.gameboj.bits.Bits.set;
import static ch.epfl.gameboj.bits.Bits.signExtend8;
import static ch.epfl.gameboj.bits.Bits.test;
import static ch.epfl.gameboj.component.cpu.Alu.add;
import static ch.epfl.gameboj.component.cpu.Alu.add16H;
import static ch.epfl.gameboj.component.cpu.Alu.add16L;
import static ch.epfl.gameboj.component.cpu.Alu.and;
import static ch.epfl.gameboj.component.cpu.Alu.bcdAdjust;
import static ch.epfl.gameboj.component.cpu.Alu.maskZNHC;
import static ch.epfl.gameboj.component.cpu.Alu.or;
import static ch.epfl.gameboj.component.cpu.Alu.rotate;
import static ch.epfl.gameboj.component.cpu.Alu.shiftLeft;
import static ch.epfl.gameboj.component.cpu.Alu.shiftRightA;
import static ch.epfl.gameboj.component.cpu.Alu.shiftRightL;
import static ch.epfl.gameboj.component.cpu.Alu.sub;
import static ch.epfl.gameboj.component.cpu.Alu.swap;
import static ch.epfl.gameboj.component.cpu.Alu.unpackValue;
import static ch.epfl.gameboj.component.cpu.Alu.xor;

import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Alu.RotDir;
import ch.epfl.gameboj.component.cpu.Opcode.Kind;
import ch.epfl.gameboj.component.memory.Ram;


public final class Cpu implements Component, Clocked {

   
    //field declarations
    private long nextNonIdleCycle = 0;
    private int PC = 0;
    private int SP = 0;
    private int IE = 0;
    private int IF = 0;
    private boolean IME = false;
    private final static int OPCODE_MAX_LENGTH = 256;

    private Bus bus;
    private Ram highRam = new Ram(AddressMap.HIGH_RAM_SIZE);

    private Reg[] regArray = Reg.values();
    private Reg16[] reg16Array = Reg16.values();

    //registerFile gathering 8 bit register values
    private RegisterFile<Reg> registerFile = new RegisterFile<>(Reg.values());
    
    //array to easily handle direct opcodes 
    private static final Opcode[] DIRECT_OPCODE_TABLE = buildOpcodeTable(
            Opcode.Kind.DIRECT);
    
    //array to easily handle prefixed opcodes 
    private static final Opcode[] PREFIXED_OPCODE_TABLE = buildOpcodeTable(
            Opcode.Kind.PREFIXED);

    
    /**
     * Builds the opcode table.
     * @param k : kind of opcode
     * @return  : opcode array for prefixed and direct opcodes
     */
    private static Opcode[] buildOpcodeTable(Kind kind) {
        
        Opcode[] codeTable = new Opcode[OPCODE_MAX_LENGTH];
        
        for (Opcode opcode : Opcode.values()) {
            if (opcode.kind == kind) {
                codeTable[opcode.encoding] = opcode;
            }
        }

        return codeTable;
    }

    /* (non-Javadoc)
     * @see ch.epfl.gameboj.component.Clocked#cycle(long)
     */
    // first checks if the halt instruction is activated and if there is an interruption to handle
    // in this case sets nextNonIdleCycle and calls reallyCycle 
    @Override
    public void cycle(long cycle) {
     
        if(nextNonIdleCycle == Long.MAX_VALUE && (findInterruption() != -1) ) { 
            nextNonIdleCycle = cycle ; 
            reallyCycle();

        }else if (nextNonIdleCycle == cycle) {
            reallyCycle();
        }

    }

    /**
     * Starts by checking if interrupts can be detected and if there is indeed an interruption(-1 meaning there is none), it handles it
     * else calls dispatch method to execute instructions as usual 
     * @param cycle the cycle
     */
    public void reallyCycle() {
        int index = findInterruption();
        if (IME && (index != -1)) {

            IME = false;
            IF = set(IF, index, false); // sets 0 to corresponding bit for handled interruption
            push16(PC);
            PC = AddressMap.INTERRUPTS[index];
            nextNonIdleCycle += 5;
        } else {
    
            if (read8(PC) == 0xCB) {
                dispatch(PREFIXED_OPCODE_TABLE[read8AfterOpcode()]);
            } else {
                dispatch(DIRECT_OPCODE_TABLE[read8(PC)]);
            }
      
           
    }

    }

    /**
     * Hands out each instruction to corresponding case and executes them .
     * @param opcode the opcode
     */
    private void dispatch(Opcode opcode) {

        int nextPC = PC + opcode.totalBytes;

        switch (opcode.family) {

        case NOP: {
        }
            break;
        case LD_R8_HLR: {
            Reg reg = extractReg(opcode, 3);
            registerFile.set(reg, read8AtHl());

        }
            break;
        case LD_A_HLRU: {

            registerFile.set(Reg.A, read8(reg16(Reg16.HL)));
            setReg16(Reg16.HL, extract(reg16(Reg16.HL) + extractHlIncrement(opcode), 0, 16));

        }
            break;
        
        case LD_A_N8R: {
            registerFile.set(Reg.A, read8(AddressMap.REGS_START + read8AfterOpcode()));
        }
            break;
        case LD_A_CR: {
            registerFile.set(Reg.A, read8(AddressMap.REGS_START + registerFile.get(Reg.C)));
        }
            break;
        case LD_A_N16R: {
            registerFile.set(Reg.A, read8(read16AfterOpcode()));
        }
            break;
        case LD_A_BCR: {
            registerFile.set(Reg.A, read8(reg16(Reg16.BC)));
        }
            break;
        case LD_A_DER: {
            registerFile.set(Reg.A, read8(reg16(Reg16.DE)));
        }
            break;
        case LD_R8_N8: {
            Reg reg = extractReg(opcode, 3);
            registerFile.set(reg, read8AfterOpcode());
        }
            break;
        case LD_R16SP_N16: {
            Reg16 reg16 = extractReg16(opcode);
            setReg16SP(reg16, read16AfterOpcode());
        }
            break;
        case POP_R16: {
            Reg16 reg16 = extractReg16(opcode);
            setReg16(reg16, pop16());

        }
            break;
        case LD_HLR_R8: {
            Reg reg = extractReg(opcode, 0);
            write8AtHl(registerFile.get(reg));

        }
            break;
        case LD_HLRU_A: {

            write8AtHl(registerFile.get(Reg.A));
            setReg16(Reg16.HL, extract(reg16(Reg16.HL) + extractHlIncrement(opcode), 0, 16));
           
        }
            break;
        case LD_N8R_A: {
            write8(AddressMap.REGS_START + read8AfterOpcode(),
                    registerFile.get(Reg.A));
        }
            break;
        case LD_CR_A: {
            write8(AddressMap.REGS_START + registerFile.get(Reg.C),
                    registerFile.get(Reg.A));
        }
            break;
        case LD_N16R_A: {
            write8(read16AfterOpcode(), registerFile.get(Reg.A));
        }
            break;
        case LD_BCR_A: {
            write8(reg16(Reg16.BC), registerFile.get(Reg.A));
        }
            break;
        case LD_DER_A: {
            write8(reg16(Reg16.DE), registerFile.get(Reg.A));
        }
            break;
        case LD_HLR_N8: {
            write8AtHl(read8AfterOpcode());
        }
            break;
        case LD_N16R_SP: {
            write16((read16AfterOpcode()), SP);
        }
            break;
        case PUSH_R16: {
            Reg16 reg16 = extractReg16(opcode);
            push16(reg16(reg16));

        }
            break;
        case LD_R8_R8: {
            Reg regR = extractReg(opcode, 3);
            Reg regS = extractReg(opcode, 0);
            registerFile.set(regR, registerFile.get(regS));

        }
            break;
        case LD_SP_HL: {
            SP = reg16(Reg16.HL);
        }
            break;

        // Add
        case ADD_A_R8: {
            int regValue = registerFile.get(extractReg(opcode, 0));
            int result = add(registerFile.get(Reg.A), regValue, combineCAndOpcode(opcode));
            setRegFlags(Reg.A, result);

        }
            break;
        case ADD_A_N8: {
            int n8 = read8AfterOpcode();
            int result = add(registerFile.get(Reg.A), n8, combineCAndOpcode(opcode));
            setRegFlags(Reg.A, result);

        }
            break;
        case ADD_A_HLR: {
            int n8_HL = read8AtHl();
            int result = add(registerFile.get(Reg.A), n8_HL, combineCAndOpcode(opcode));
            setRegFlags(Reg.A, result);

        }
            break;
        case INC_R8: {
            Reg reg = extractReg(opcode, 3);
            int regValue = registerFile.get(reg);
            int newRegValue = add(regValue, 1); 
            registerFile.set(reg, unpackValue(newRegValue));
            combineAluFlags(newRegValue, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU,
                    FlagSrc.CPU);

        }
            break;
        case INC_HLR: {
            int n8_HL = read8AtHl();
            int newHlValue = add(n8_HL, 1); 
            write8AtHl(unpackValue(newHlValue));
            combineAluFlags(newHlValue, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.CPU);

        }
            break;
        case INC_R16SP: {
            Reg16 reg16 = extractReg16(opcode);
            int result = getReg16SP(reg16);
            result = add16H(result, 1);
            setReg16SP(reg16, unpackValue(result));
        }
            break;
        case ADD_HL_R16SP: {
            Reg16 reg16 = extractReg16(opcode);
            int value = getReg16SP(reg16);
            int valueHL = reg16(Reg16.HL);
            int result = add16H(valueHL, value);
            setReg16SP(Reg16.HL, unpackValue(result));
            combineAluFlags(result, FlagSrc.CPU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.ALU);
        }
            break;
        case LD_HLSP_S8: {
            int e = Bits.clip(16, Bits.signExtend8(read8AfterOpcode()));

            int result = add16L(SP, e);
            if (test(opcode.encoding, 4)) {

                setReg16(Reg16.HL, unpackValue(result));

            } else {
                SP = unpackValue(result);
            }

            combineAluFlags(result, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU,
                    FlagSrc.ALU);

        }
            break;

        // Subtract
        case SUB_A_R8: {
            int regValue = registerFile.get(extractReg(opcode, 0));
            int result = sub(registerFile.get(Reg.A), regValue, combineCAndOpcode(opcode));
            setRegFlags(Reg.A, result);

        }
            break;
        case SUB_A_N8: {
            int result = sub(registerFile.get(Reg.A), read8AfterOpcode(), combineCAndOpcode(opcode));
            setRegFlags(Reg.A, result);
        }
            break;
        case SUB_A_HLR: {
            int n8_HL = read8AtHl();
            int result = sub(registerFile.get(Reg.A), n8_HL, combineCAndOpcode(opcode));
            setRegFlags(Reg.A, result);

        }
            break;
        case DEC_R8: {
            Reg reg = extractReg(opcode, 3);
            int regValue = registerFile.get(reg);
            int newRegValue = sub(regValue, 1);
            registerFile.set(reg, unpackValue(newRegValue));
            combineAluFlags(newRegValue, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.CPU);

        }
            break;
        case DEC_HLR: {
            int n8_HL = read8AtHl();
            int result = sub(n8_HL, 1);
            write8AtHl(unpackValue(result));
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU,
                    FlagSrc.CPU);

        }
            break;
        case CP_A_R8: {
            int regValue = registerFile.get(extractReg(opcode, 0));
            int result = sub(registerFile.get(Reg.A), regValue);
            setFlags(result);
        }
            break;
        case CP_A_N8: {
            int result = sub(registerFile.get(Reg.A), read8AfterOpcode() );
            setFlags(result);

        }
            break;
        case CP_A_HLR: {
            int n8_HL = read8AtHl();
            int result = sub(registerFile.get(Reg.A), n8_HL);
            setFlags(result);

        }
            break;
        
        case DEC_R16SP: {
            Reg16 reg16 = extractReg16(opcode);
            int result = getReg16SP(reg16) - 1;
            result = Bits.clip(16, result);
            setReg16SP(reg16, result);
        }
            break;

        // And, or, xor, complement
        case AND_A_N8: {

            int conjunction = and(registerFile.get(Reg.A), read8AfterOpcode());
            setRegFlags(Reg.A, conjunction);

        }
            break;
        case AND_A_R8: {
            Reg reg = extractReg(opcode, 0);
            int conjunction = and(registerFile.get(Reg.A), registerFile.get(reg));
            setRegFlags(Reg.A, conjunction);

        }
            break;
        case AND_A_HLR: {
            int conjunction = and(registerFile.get(Reg.A), read8AtHl());
            setRegFlags(Reg.A, conjunction);
           
        }
            break;
        case OR_A_R8: {
            Reg reg = extractReg(opcode, 0);
            int disjunction = or(registerFile.get(Reg.A), registerFile.get(reg));
            setRegFlags(Reg.A, disjunction);

        }
            break;
        case OR_A_N8: {
            int disjunction = or(registerFile.get(Reg.A), read8AfterOpcode());
            setRegFlags(Reg.A, disjunction);
        }
            break;
        case OR_A_HLR: {
            int disjunction = or(registerFile.get(Reg.A), read8AtHl());
            setRegFlags(Reg.A, disjunction);
        }
            break;
        case XOR_A_R8: {
            Reg reg = extractReg(opcode, 0);
            int exDisjunction = xor(registerFile.get(Reg.A), registerFile.get(reg));
            setRegFlags(Reg.A, exDisjunction);

        }
            break;
        case XOR_A_N8: {
            int exDisjunction = xor(registerFile.get(Reg.A), read8AfterOpcode());
            setRegFlags(Reg.A, exDisjunction);

        }
            break;
        case XOR_A_HLR: {
            int exDisjunction = xor(registerFile.get(Reg.A), read8AtHl());
            setRegFlags(Reg.A, exDisjunction);
        }
            break;
        case CPL: {
            int complement = complement8((registerFile.get(Reg.A)));
            registerFile.set(Reg.A, complement);
            combineAluFlags(complement, FlagSrc.CPU, FlagSrc.V1, FlagSrc.V1, FlagSrc.CPU);

        }
            break;

        // Rotate, shift
        case ROTCA: {
            RotDir direction = extractDirection(opcode);
            int rotated = rotate(direction, registerFile.get(Reg.A));          
            setRegFromAlu(Reg.A, rotated);
            combineAluFlags(rotated, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0,
                    FlagSrc.ALU);

        }
            break;
        case ROTA: {
            RotDir direction;
            direction = extractDirection(opcode);
            int rotated = rotate(direction, registerFile.get(Reg.A), test(registerFile.get(Reg.F), 4));
            setRegFromAlu(Reg.A, rotated);
            combineAluFlags(rotated, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);

        }
            break;
        case ROTC_R8: {
            Reg reg = extractReg(opcode, 0);
            RotDir direction = extractDirection(opcode);
            int rotated = rotate(direction, registerFile.get(reg));
            setRegFlags(reg, rotated);

        }
            break;
        case ROT_R8: {
            Reg reg = extractReg(opcode, 0);
            RotDir direction = extractDirection(opcode);
            int rotated = rotate(direction, registerFile.get(reg), test(registerFile.get(Reg.F), 4));
            setRegFlags(reg, rotated);
        }
            break;
        case ROTC_HLR: {
            RotDir direction = extractDirection(opcode);
            int rotated = rotate(direction, read8AtHl());
            write8AtHlAndSetFlags(rotated);

        }
            break;
        case ROT_HLR: {
            RotDir direction = extractDirection(opcode);
            int rotated = rotate(direction, read8AtHl(), test(registerFile.get(Reg.F), 4));
            write8AtHlAndSetFlags(rotated);

        }
            break;
        case SWAP_R8: {
            Reg reg = extractReg(opcode, 0);
            int swapped = swap(registerFile.get(reg));
            setRegFlags(reg, swapped);

        }
            break;
        case SWAP_HLR: {
            int swapped = swap(read8AtHl());
            write8AtHlAndSetFlags(swapped);
        }
            break;
        case SLA_R8: {
            Reg reg = extractReg(opcode, 0);
            int shifted = shiftLeft(registerFile.get(reg));
            setRegFlags(reg, shifted);

        }
            break;
        case SRA_R8: {
            Reg reg = extractReg(opcode, 0);
            int shifted = shiftRightA(registerFile.get(reg));
            setRegFlags(reg, shifted);

        }
            break;
        case SRL_R8: {
            Reg reg = extractReg(opcode, 0);
            int shifted = shiftRightL(registerFile.get(reg));
            setRegFlags(reg, shifted);
        }
            break;
        case SLA_HLR: {
            int shifted = shiftLeft(read8AtHl());
            write8AtHlAndSetFlags(shifted);

        }
            break;
        case SRA_HLR: {
            int shifted = shiftRightA(read8AtHl());
            write8AtHlAndSetFlags(shifted);

        }
            break;
        case SRL_HLR: {
            int shifted = shiftRightL(read8AtHl());
            write8AtHlAndSetFlags(shifted);
        }
            break;

        // Bit test and set
        case BIT_U3_R8: {
            Reg reg = extractReg(opcode, 0);
            int index = extractIndex(opcode);
            boolean z = !test(registerFile.get(reg), index);
            registerFile.set(Reg.F,
                    maskZNHC(z, false, true, test(registerFile.get(Reg.F), 4))); //test calls to determine flags

        }
            break;
        case BIT_U3_HLR: {
            int index = extractIndex(opcode);
            boolean z = !test(read8AtHl(), index);
            registerFile.set(Reg.F,
                    maskZNHC(z, false, true, test(registerFile.get(Reg.F), 4)));

        }
            break;
        case CHG_U3_R8: {
            Reg reg = extractReg(opcode, 0);
            int index = extractIndex(opcode);
            int setValue = 0;
            if (test(opcode.encoding, 6)) {
                // case of set
                setValue = setResetIndex(registerFile.get(reg), index, true);
                registerFile.set(reg, setValue); 
            } else { 
                // case of reset
                setValue = setResetIndex(registerFile.get(reg), index, false);
                registerFile.set(reg, setValue);
            }
           

        }
            break;
        case CHG_U3_HLR: {
            int index = extractIndex(opcode);
            int setValue = 0;
            if (test(opcode.encoding, 6)) { // case of set
                setValue = setResetIndex(read8AtHl(), index, true);
                write8AtHl(setValue);
            } else {
                //case of reset
                setValue = setResetIndex(read8AtHl(), index, false);
                write8AtHl(setValue);
            }

        }
            break;

        // Misc. ALU 
        case DAA: {
            boolean n = test(registerFile.get(Reg.F), 6);
            boolean h = test(registerFile.get(Reg.F), 5);
            boolean c = test(registerFile.get(Reg.F), 4);
            int adjusted = bcdAdjust(registerFile.get(Reg.A), n, h, c);

            setRegFromAlu(Reg.A, adjusted);
            combineAluFlags(adjusted, FlagSrc.ALU, FlagSrc.CPU, FlagSrc.V0, FlagSrc.ALU);
        }
            break;
        case SCCF: {
            if (test(opcode.encoding, 4)) {
                registerFile.set(Reg.F,
                        maskZNHC(test(registerFile.get(Reg.F), 7), false, false, !combineCAndOpcode(opcode)));
            } else {
                combineAluFlags(registerFile.get(Reg.F), FlagSrc.CPU,
                        FlagSrc.V0, FlagSrc.V0, FlagSrc.V1);
            }
        }
            break;
 
        // Jumps
        case JP_HL: {
            nextPC = reg16(Reg16.HL);
        }
            break;
        case JP_N16: {

            nextPC = read16AfterOpcode();
        }
            break;
        case JP_CC_N16: {
            if(checkCondition(opcode)) {
                nextNonIdleCycle += opcode.additionalCycles;
                nextPC = read16AfterOpcode();
            }
        }
            break;
        case JR_E8: {
            
            int e8 = clip(16, signExtend8((read8AfterOpcode())));
            nextPC = clip(16, nextPC + e8);

        }
            break;
        case JR_CC_E8: {
            if(checkCondition(opcode)) {
                nextNonIdleCycle += opcode.additionalCycles;
                int e8 = clip(16, signExtend8((read8AfterOpcode()))); 
                nextPC = clip(16, nextPC + e8);
 
            }
        }
            break;
        // Calls and returns
        case CALL_N16: {
            push16(nextPC);
            nextPC = read16AfterOpcode();
            
        }
            break;
        case CALL_CC_N16: {
            if(checkCondition(opcode)) {
                nextNonIdleCycle += opcode.additionalCycles;
                push16(nextPC);
                nextPC = read16AfterOpcode();
            }
        }
            break;
        case RST_U3: {
            int index = extractIndex(opcode);
            push16(nextPC);
            nextPC = AddressMap.RESETS[index];
        }
            break;
        case RET: {
            nextPC = pop16();
        }
            break;
        case RET_CC: {
            if(checkCondition(opcode)) {
                nextNonIdleCycle += opcode.additionalCycles;
                nextPC = pop16();
            }
        }
            break;
        // Interrupts
        case EDI: {
            IME = test(opcode.encoding, 3) ;

        }
            break;
        case RETI: {
            IME = true;
            nextPC = pop16();
        }
            break;
        // Misc control
        case HALT: {                
            nextNonIdleCycle = Long.MAX_VALUE;
        }
            break;
        case STOP:
            throw new Error("STOP is not implemented");
        }


        PC = clip(16, nextPC);
        nextNonIdleCycle += opcode.cycles;

    }
    
    private int read8(int address) {
        return bus.read(address);
    }

    private int read8AtHl() {

        int address = reg16(Reg16.HL);
        return read8(address);

    }

    private int read8AfterOpcode() {
        return read8(PC + 1);
    }

    private int read16(int address) {
        int lowBits = read8(address);
        int highBits = read8(address + 1);

        return make16(highBits, lowBits);
    }

    private int read16AfterOpcode() {
        int lowBits = read8(PC + 1);
        int highBits = read8(PC + 2);

        return make16(highBits, lowBits);

    }

    private void write8(int address, int v) {
        bus.write(address, v);
    }

    private void write16(int address, int v) {
        bus.write(address, extract(v, 0, 8));
        bus.write(address + 1, extract(v, 8, 8));
    }

    private void write8AtHl(int v) {

        write8(reg16(Reg16.HL), v);
    }

    private void push16(int v) {
        SP = extract(SP - 2, 0, 16);

        bus.write(SP, extract(v, 0, 8)); //to get least significant 8 bits
        bus.write(SP + 1, extract(v, 8, 8)); //to get most significant 8 bits

    }

    private int pop16() {

        int pop = read16(SP);
        SP = extract(SP + 2, 0, 16);

        return pop;
    }


    // Dealing with register couples
    private int reg16(Reg16 r) {
        int lowBits = 0;
        int highBits = 0;
        for (int i = 0; i < reg16Array.length; ++i) {
            if (r == reg16Array[i]) {
                highBits = registerFile.get(regArray[2 * i]);
                lowBits = registerFile.get(regArray[2 * i + 1]);
            }
        }
        return make16(highBits, lowBits);
    }

    private void setReg16(Reg16 r, int newV) {
        newV = checkBits16(newV);

        int highBits = extract(newV, 8, 8);
        int lowBits = extract(newV, 0, 8);

        if (r == Reg16.AF) {
            int zeroConverterF = lowBits & 0b1111_0000;

            registerFile.set(Reg.A, highBits);
            registerFile.set(Reg.F, zeroConverterF);

        }

        for (int i = 1; i < reg16Array.length; ++i) {
            if (r == reg16Array[i]) {
                registerFile.set(regArray[2 * i], highBits);
                registerFile.set(regArray[2 * i + 1], lowBits);

            }
        }

    }

    //if the argument reg16 is AF , treats that as SP otherwise calls 
    //the usual setReg16
    private void setReg16SP(Reg16 reg16, int newV) {
        newV = checkBits16(newV);
        if (reg16 == Reg16.AF) {
            SP = newV;
        } else {
            setReg16(reg16, newV);
        }
    }


    /**
     * @return an array to simplify usage of tests, containing PS, SP and flags
     */
    public int[] _testGetPcSpAFBCDEHL() {
        int[] testArray = new int[10];
        testArray[0] = PC;
        testArray[1] = SP;
        int i = 2;

        for (Reg r : regArray) {
            testArray[i] = registerFile.get(r);
            ++i;
        }

        return testArray;
    }

    /**
     * @param opcode: opcode of the instruction
     * @param startBit: index that bits start from
     * @return : using the table in the instructions, returns identity of 
     * an 8 bits register, notice that all numbers shifted by 2 except the register A
     */
    private Reg extractReg(Opcode opcode, int startBit) {
        Reg reg = null;
        int position = extract(opcode.encoding, startBit, 3);
        if (position == 7) {
            reg = Reg.A;
        }

        else if (position != 6) {
            reg = regArray[position + 2];
        }

        return reg;
    }


    /**
     * @param opcode: opcode of the instruction
     * @return : does the same thing with extractReg but for the pairs of registers
     *  this time using the table in the instructions
     */
    private Reg16 extractReg16(Opcode opcode) {
        int position = extract(opcode.encoding, 4, 2);
        return reg16Array[(position + 1) % reg16Array.length];
    }

    private int extractHlIncrement(Opcode opcode) {
        if (test(opcode.encoding, 4)) {
            return -1;
        }
        return 1;
    }


    private void setRegFromAlu(Reg r, int vf) {
        registerFile.set(r, Alu.unpackValue(vf));
    }

    private void setFlags(int valueFlags) {
        registerFile.set(Reg.F, Alu.unpackFlags(valueFlags));
    }


    private void setRegFlags(Reg r, int vf) {
        setRegFromAlu(r, vf);
        setFlags(vf);
    }

    private void write8AtHlAndSetFlags(int vf) {
        write8AtHl(Alu.unpackValue(vf));
        setFlags(vf);
    }
    

    private void combineAluFlags(int vf, FlagSrc z, FlagSrc n, FlagSrc h,
            FlagSrc c) {
        int zValue = compareFlag(vf, z, 7);
        int nValue = compareFlag(vf, n, 6);
        int hValue = compareFlag(vf, h, 5);
        int cValue = compareFlag(vf, c, 4);
        int result = zValue + nValue + hValue + cValue;
        registerFile.set(Reg.F, result);

    }

    private int compareFlag(int vf, FlagSrc fs, int index) {
        int value = 1 << index;

        if (fs == FlagSrc.V0) {
            value = 0;
        }

        else if (fs == FlagSrc.ALU) {
            value = Alu.unpackFlags(vf) & value;
        }

        else if (fs == FlagSrc.CPU) {
            value = registerFile.get(Reg.F) & value;
        }

        return value;
    }

    
    private enum FlagSrc {

        V0, V1, ALU, CPU

    }

    private RotDir extractDirection(Opcode opcode) {
        if (test(opcode.encoding, 3)) {
            return RotDir.RIGHT;
        } else {
            return RotDir.LEFT;
        }
    }

    private int extractIndex(Opcode opcode) {
        int index = extract(opcode.encoding, 3, 3);
        return index;
    }

    private int setResetIndex(int bits, int index, boolean newValue) {
        return Bits.set(bits, index, newValue);
    }

    private boolean combineCAndOpcode(Opcode opcode) {

        int encoding = opcode.encoding;
        boolean b = test(encoding, 3);
        boolean c = test(registerFile.get(Reg.F), 4);
        return b & c;
    }
    
    private int getReg16SP(Reg16 r16) {
        if (r16 == Reg16.AF) {
            return SP;
        } else {
            return reg16(r16);
        }
    }
     
   
    /**
     * @param i : interruption
     *  By setting true the index of wanted interruption, 
     *  we invoke an interruption
     */
    public void requestInterrupt(Interrupt i) {     //bu kesin public mi 
       IF = set(IF ,i.index(), true);                         
    }
    
    /**
     * @param opcode : opcode of the instruction
     * @return : the boolean value indicates whether the nextNonIdlecycle should
     * be updated or not using the table in the instructions.( if condition is
     * smaller than 7th bit is examined and else the 4th. if the condition is even
     * than we negate our boolean) 
     */
    private boolean checkCondition(Opcode opcode) {         
        int condition = extract(opcode.encoding, 3, 2);
        boolean flag;
        if(condition < 0b10) {
            flag = test(registerFile.get(Reg.F), 7);
        }
        
        else {
            flag = test(registerFile.get(Reg.F), 4);
        }
        
        if(condition % 2 == 0) {
            flag = !flag;
        }
        return flag;
     }
   
    private int findInterruption() {
        int conjunctionIE_IF = IE & IF;
        if(conjunctionIE_IF == 0) {
            return -1;
        }else {
        
            return  Integer.numberOfTrailingZeros(conjunctionIE_IF);  
        //numberOfTrailingZeros turns 32 in case conjunctionIE_IF
        //is 0 so we handled that with if block above
        }
    }

    private enum Reg implements Register {

        A, F, B, C, D, E, H, L
    }

    private enum Reg16 implements Register {

        AF, BC, DE, HL
    }
    
    
     //Interruption types
    public enum Interrupt implements Bit {

        VBLANK, LCD_STAT, TIMER, SERIAL, JOYPAD

    }

    /* (non-Javadoc)
     * @see ch.epfl.gameboj.component.Component#attachTo(ch.epfl.gameboj.Bus)
     */
    //calling the super method from implemented interface Component
    //enabling attachment to bus
    @Override
    public void attachTo(Bus bus) {
        Objects.requireNonNull(bus);
        this.bus = bus;
        Component.super.attachTo(bus);

    }

    /* (non-Javadoc)
     * @see ch.epfl.gameboj.component.Component#read(int)
     */
    //first checking if address is one of IE or IF and returns those if needed,
    //else checking if address is in range of highRam and then reading on index (which
    //is address - HIGH_RAM_START
    //if not in range returns NO_DATA
    @Override
    public int read(int address) {
       address = checkBits16(address);
       if(address == AddressMap.REG_IE) {
           return IE;
       }else if(address == AddressMap.REG_IF){
           return IF;
       }else if(address >= AddressMap.HIGH_RAM_START && address <  AddressMap.HIGH_RAM_END) {
           int index = address - AddressMap.HIGH_RAM_START;
           return highRam.read(index);
       }else
           return Component.NO_DATA;
    }

    /* (non-Javadoc)
     * @see ch.epfl.gameboj.component.Component#write(int, int)
     */
    //first checking if address is one of IE or IF and gives data value to those
    //then checking if address is in range of highRam and then writing data on index 
    // which is address - HIGH_RAM_START           
    @Override
    public void write(int address, int data) {
        address = checkBits16(address);
        data = checkBits8(data);
       
        if(address == AddressMap.REG_IE) {
            IE = data;
        }else if(address == AddressMap.REG_IF){
            IF = data;
        }else if(address >= AddressMap.HIGH_RAM_START && address <  AddressMap.HIGH_RAM_END) {
            int index = address -  AddressMap.HIGH_RAM_START;
            highRam.write(index, data);
        }
    }
}