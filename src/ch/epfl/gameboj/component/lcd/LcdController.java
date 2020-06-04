package ch.epfl.gameboj.component.lcd;

import static ch.epfl.gameboj.Preconditions.checkArgument;
import static ch.epfl.gameboj.Preconditions.checkBits16;
import static ch.epfl.gameboj.Preconditions.checkBits8;
import static ch.epfl.gameboj.bits.Bits.extract;
import static ch.epfl.gameboj.bits.Bits.make16;
import static ch.epfl.gameboj.bits.Bits.reverse8;
import static ch.epfl.gameboj.bits.Bits.test;

import java.util.Arrays;
import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;
import ch.epfl.gameboj.component.memory.Ram;

public final class LcdController implements Clocked, Component {

    
    //CONSTANTS
    public static final int LCD_WIDTH = 160;
    public static final int LCD_HEIGHT = 144;
    private static final int WINDOW_LENGTH = 256;   
    private static final int CYCLES_IN_MODE_2 = 20;
    private static final int CYCLES_IN_MODE_3 = 43;
    private static final int CYCLES_IN_MODE_0 = 51;
    private static final int CYCLES_PER_LINE = CYCLES_IN_MODE_2 + CYCLES_IN_MODE_3 + CYCLES_IN_MODE_0;
    private static final int VBLANK_NUMBER = 10;
    private static final int TILE_PIXEL_LENGTH = 8;
    private static final int TILE_NUMBER =  WINDOW_LENGTH / TILE_PIXEL_LENGTH;
    private static final int WX_ERROR = 7;
    private static final int OVERFLOW_8BITS = 0x80;
    private static final int SMALL_SPRITE_PIXEL_HEIGHT = 8;
    private static final int BIG_SPRITE_PIXEL_HEIGHT = 16;
    private static final int SPRITE_FIELD_NUMBER = 4;
    private static final int MAX_SPRITE_NUMBER = 40;
    private static final int COOR_Y_ADJUSTER = 16;
    private static final int COOR_X_ADJUSTER = 8;
    private static final int MAX_SPRITE_IN_LINE = 10;
    private static final int SPRITE_CHARACTERISTICS_BYTE = 3;
    private static final int MSB_AND_LSB_PIXELS = 16;
    private static final int SMALL_SPRITE_INVERTER = 7;
    private static final int BIG_SPRITE_INVERTER = 15;
    private static final int NONWRITABLE_BITS = 3;
    
    // FIELDS
    private final Ram videoRam;
    private final Ram OAM;
    private final Cpu gameboyCpu;
    private LcdImage currentImage;
    private LcdImage.Builder nextImageBuilder;
    private Bus bus;
    private long nextNonIdleCycle = Long.MAX_VALUE;
    private long lcdOnCycle = 0;
    private int winY = 0;
    private int fastCopySourceAddress = 0;
    private int fastCopyDestinationAddress = AddressMap.OAM_END; 
    private final LcdReg[] lcdRegArray = LcdReg.values();
    private final RegisterFile<LcdReg> lcdRegFile = new RegisterFile<>(lcdRegArray);
    

    /**
     * enumeration on registers of LcdController giving information on drawing 
     * background, window and sprite as well as status of LCD
     */
    private enum LcdReg implements Register{
        LCDC, STAT, SCY , SCX , LY, LYC, DMA,  BGP, OBP0, OBP1, WY, WX
    }  
    
    /**
     * enumeration on bits of LCDC register, containing several information for drawing
     */
    private enum LCDCReg implements Bit{
        BG, OBJ ,OBJ_SIZE, BG_AREA, TILE_SOURCE, WIN, WIN_AREA, LCD_STATUS
    }
    
    /**
     * enumeration on bits of STAT register, containing several 
     * information for interruptions to be requested
     * 
     */
    private enum STATReg implements Bit{
        MODE0, MODE1, LYC_EQ_LY, INT_MODE0, INT_MODE1,INT_MODE2, INT_LYC
    }
    
    /**
     * enumeration for changes on sprite
     * for clarifying code and avoiding constant usage)
     */
    private enum SpriteByteCharacteristics implements Bit {
        UNUSED0, UNUSED1, UNUSED2, UNUSED3, PALETTE, FLIP_H, FLIP_V, BEHIND_BG
    }
   
    /**
     * enumeration on where to find several sprite information
     * (for clarifying code and avoiding constant usage) 
     */
    private enum SpriteInformation implements Bit {
        COOR_Y, COOR_X, TILE_INDEX, CHARACTERISTICS
    }
    
    /**
     * Modes enumeration to dispatch in reallyCycle and to set STAT bits
     */
    private enum Modes {
        MODE_0, MODE_1, MODE_2, MODE_3
    }

    /**
     * @param gameboyCpu : takes this parameter in order to request interruptions
     * two necessary rams(OAM and videoRam) are created with proper size, 
     * giving maximal long value to nextNonIdleCycle to signal that screen is
     * initially off, builder is instantiated as well
     */
    public LcdController(Cpu gameboyCpu) {
       Objects.requireNonNull(gameboyCpu);
       this.gameboyCpu = gameboyCpu; 
       nextImageBuilder = new LcdImage.Builder(LCD_WIDTH,LCD_HEIGHT);
       currentImage = nextImageBuilder.build();
       videoRam = new Ram(AddressMap.VIDEO_RAM_SIZE);
       OAM = new Ram(AddressMap.OAM_RAM_SIZE);
       nextNonIdleCycle = Long.MAX_VALUE;
    }

    /* (non-Javadoc)
     * @see ch.epfl.gameboj.component.Clocked#cycle(long)
     * in first if, fast copy process is handled, if fastCopyDestinationAddress 
     * is not arrived until end of OAM, process continues and proper values are
     * read from bus, in second if normal drawing process happens if the screen is on,
     * finally the wake process is written, mode is set to beginning which is 2,
     * nextNonIdleCycle and lcdOnCycle are set to cycle in order to get in right mode
     */
    @Override
    public void cycle(long cycle) {

        if(fastCopyDestinationAddress != AddressMap.OAM_END) {
            write(fastCopyDestinationAddress, bus.read(fastCopySourceAddress)); 
            ++fastCopySourceAddress;
            ++fastCopyDestinationAddress;
        }
        if(nextNonIdleCycle == cycle && lcdRegFile.testBit(LcdReg.LCDC, LCDCReg.LCD_STATUS)) { 
            reallyCycle(cycle);
        }
        
        else if(nextNonIdleCycle == Long.MAX_VALUE && lcdRegFile.testBit(LcdReg.LCDC, LCDCReg.LCD_STATUS)){ 
            setMode(Modes.MODE_2);        
            nextNonIdleCycle = cycle; 
            lcdOnCycle = cycle;
            reallyCycle(cycle);
           
        }

    }
    
  /**
 * @param cycle : general cycle number, this parameter is used to 
 * compute cycles after last wake process(cycleNumber), lineNumber which is from 
 * 0 to 153 to determine whether we are in mode vblank or not, finally using 
 * the helper method getMode places in right mode
 */
public void reallyCycle(long cycle) {
  long cycleNumber = (cycle - lcdOnCycle) % ((VBLANK_NUMBER + LCD_HEIGHT) * CYCLES_PER_LINE);
  int lineNumber = (int) (cycleNumber / CYCLES_PER_LINE);
  int inLineCycleNumber = (int) (cycleNumber % CYCLES_PER_LINE);
       
  switch (getMode(inLineCycleNumber, lineNumber)) {

        //requesting proper interruption(lcd_stat), setting mode and nextNonIdleCycle
        case MODE_0: {

            setMode(Modes.MODE_0); 

            if (lcdRegFile.testBit(LcdReg.STAT, STATReg.INT_MODE0)) {
                gameboyCpu.requestInterrupt(Interrupt.LCD_STAT);
            }
            nextNonIdleCycle += CYCLES_IN_MODE_0;
        }
            break;
            
         //requesting proper interruptions(vblank and lcd_stat), setting mode and nextNonIdleCycle
         //in this mode lineNumber is incremented so we modify LY
         //if end of vblank is reached image is built 
        case MODE_1: {
            setMode(Modes.MODE_1); 
            modifyLY(lineNumber);

            if (lineNumber == LCD_HEIGHT) {
                gameboyCpu.requestInterrupt(Interrupt.VBLANK);
                currentImage = nextImageBuilder.build();
            }
            if (lcdRegFile.testBit(LcdReg.STAT, STATReg.INT_MODE1)) {
                gameboyCpu.requestInterrupt(Interrupt.LCD_STAT);
            }

            nextNonIdleCycle += CYCLES_PER_LINE;
        }
            break;
        
        //requesting proper interruption(lcd_stat), setting mode and nextNonIdleCycle
        //in range 0 to 143 we increment LY with modifyLY method
        //if we are at the beginning of an image, a new builder is created also
        //y coordinate of window is set to 0
        case MODE_2: {

            setMode(Modes.MODE_2); 
            modifyLY(lineNumber);

            if (lineNumber == 0) {
                nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
                winY = 0;
            }

            if (lcdRegFile.testBit(LcdReg.STAT, STATReg.INT_MODE2)) {
                gameboyCpu.requestInterrupt(Interrupt.LCD_STAT);
            }
            nextNonIdleCycle += CYCLES_IN_MODE_2;
        }
            break;

        //requesting proper interruption(lcd_stat), setting mode and nextNonIdleCycle
        //computed lines are being added to the builder        
        case MODE_3: {

            setMode(Modes.MODE_3); 
            nextImageBuilder.setLine(lineNumber, computeLine(lineNumber));
            nextNonIdleCycle += CYCLES_IN_MODE_3;

        }
            break;
        }
    }

    /* (non-Javadoc)
     * @see ch.epfl.gameboj.component.Component#read(int)
     * giving proper accesses to videoRam and OAM ram
     * if the address corresponds to one of Lcd Registers it reads from enum array 
     */
    @Override
    public int read(int address) {
        checkBits16(address);
        if (address >= AddressMap.VIDEO_RAM_START && address < AddressMap.VIDEO_RAM_END) {
            address -= AddressMap.VIDEO_RAM_START;
            return videoRam.read(address);
        } 
        if (address >= AddressMap.REGS_LCDC_START && address < AddressMap.REGS_LCDC_END) {
            address -=  AddressMap.REGS_LCDC_START;
            return getReg(lcdRegArray[address]);       
        }
        if(address >= AddressMap.OAM_START && address < AddressMap.OAM_END) {
            address -=  AddressMap.OAM_START;
            return OAM.read(address);
        }
        return NO_DATA;
    }

    /* (non-Javadoc)
     * @see ch.epfl.gameboj.component.Component#write(int, int)
     * writes data on corresponding address' of OAM or videoRam
     * if it is one of Lcd Registers special cases exist (described below)
     * 
     */
    @Override
    public void write(int address, int data) {
        checkBits16(address);
        checkBits8(data);
        if (address >= AddressMap.VIDEO_RAM_START && address < AddressMap.VIDEO_RAM_END) {
            address -= AddressMap.VIDEO_RAM_START;
            videoRam.write(address, data);
        } 
        else if(address >= AddressMap.OAM_START && address < AddressMap.OAM_END) {
            address -= AddressMap.OAM_START;
            OAM.write(address, data);
        }
        else if(address >= AddressMap.REGS_LCDC_START && address < AddressMap.REGS_LCDC_END) {
            address -= AddressMap.REGS_LCDC_START;
            LcdReg currentReg = lcdRegArray[address];
            
            if(currentReg == LcdReg.LCDC) {  
                setReg(LcdReg.LCDC, data); 
                
                //here we check if LCD is to be set off, if so
                //we set nextNonIdleCycle to the maximal value, setting LY to zero 
                //because a new image will start at the wake process and finally setting mode to 0
                if (!lcdRegFile.testBit(LcdReg.LCDC, LCDCReg.LCD_STATUS)) {            
                    nextNonIdleCycle = Long.MAX_VALUE;
                    modifyLY(0); //forcing LY to 0
                    setMode(Modes.MODE_0);                     
                }
            }
            
            //we are extracting three least significant bits of STAT, making    
            //three lsb bits of data to zero and taking the conjunction, so that first three
            //bits of STAT remains unchanged
            else if(currentReg == LcdReg.STAT) { 
                int firstThreeBits = extract(getReg(LcdReg.STAT), 0, NONWRITABLE_BITS);
                data = data >>> NONWRITABLE_BITS;
                data = data << NONWRITABLE_BITS;
                data = data | firstThreeBits;
                setReg(LcdReg.STAT, data);
            }
            //here we modify LY with helper method which calls another helper method
            //for controlling if an interruption needs to be requested
            else if(currentReg == LcdReg.LYC) {
                modifyLYC(data);
            }
            
            //here fast copy process is initiated, fastCopySourceAddress is formed from
            //the value written on DMA and destination address is the beginning of OAM
            else if(currentReg == LcdReg.DMA) {
                setReg(LcdReg.DMA, data);
                fastCopySourceAddress = make16(data, 0);
                fastCopyDestinationAddress = AddressMap.OAM_START;
            }      
            //this is to limit any writing on LY
                if(currentReg != LcdReg.LY) {
                setReg(lcdRegArray[address], data);  
            }
        }
    }

    /**
     * @return the last image built
     * if null builds and returns an empty image
     */
    public LcdImage currentImage() {
        if (currentImage == null) {
            LcdImage.Builder builder = new LcdImage.Builder(LCD_WIDTH,LCD_HEIGHT);
            LcdImageLine line;
            for (int i = 0; i < LCD_HEIGHT; ++i) {
                line = new LcdImageLine.Builder(LCD_WIDTH).build();
                builder.setLine(i, line);
            }
            return builder.build();
        }
        checkArgument(currentImage.width() == LCD_WIDTH && currentImage.height() == LCD_HEIGHT);
        return currentImage;
    }

    /**
     * @param index : index of the line
     * @return the computed line
     * starts out with and empty line, then according the information of
     * register it combines empty line with background image window
     * and finally with sprites
     */
    private LcdImageLine computeLine(int index) {
        checkArgument(index >= 0 && index < WINDOW_LENGTH);
        
        LcdImageLine emptyLine = new LcdImageLine.Builder(LCD_WIDTH).build();
        LcdImageLine backgroundImageLine = new LcdImageLine.Builder(LCD_WIDTH).build();
        LcdImageLine actualLine;
        LcdImageLine windowImageLine;
        BitVector opacityAdjuster;
        
        int adjustedWX = getReg(LcdReg.WX) - WX_ERROR;
        actualLine = emptyLine;
  
        
        if(lcdRegFile.testBit(LcdReg.LCDC, LCDCReg.BG)){
          backgroundImageLine = getLine((index + getReg(LcdReg.SCY)), LCDCReg.BG_AREA)
          .extractWrapped(getReg(LcdReg.SCX), LCD_WIDTH)
          .mapColors(getReg(LcdReg.BGP));
         
          actualLine = LcdImageLine.below(actualLine,backgroundImageLine);
    
        }
        if(windowEnabled(adjustedWX) && getReg(LcdReg.WY) <= index) {

            windowImageLine = getLine(winY, LCDCReg.WIN_AREA)
                   .extractZeroExtended(0, LCD_WIDTH).shift(adjustedWX)
                   .mapColors(getReg(LcdReg.BGP));
            ++winY;
            actualLine = LcdImageLine.join(windowImageLine,actualLine, (LCD_WIDTH - adjustedWX));
        }
           
         if(lcdRegFile.testBit(LcdReg.LCDC, LCDCReg.OBJ)) {
             LcdImageLine spriteLineBG = getSpriteLine(index, true);
             LcdImageLine spriteLineFG = getSpriteLine(index, false);
             
            opacityAdjuster = spriteLineBG.opacity().and(actualLine.opacity().not());            
            actualLine = LcdImageLine.below(spriteLineBG, actualLine, opacityAdjuster);          
            actualLine = LcdImageLine.below(spriteLineFG, actualLine);
          
        }
       return actualLine;
       
    }
    /**
     * @param index : index of line to compute
     * @param areaRegister : WIN_AREA or BG_AREA giving the information that tells
     * from which range we should fetch the lsb and msb
     * @return the computed line (of window or background)
     */
    private LcdImageLine getLine(int index, LCDCReg areaRegister){
        
        LcdImageLine.Builder imageBuilder = new LcdImageLine.Builder(WINDOW_LENGTH);
        int outerTileIndex = (((index) / TILE_PIXEL_LENGTH) * TILE_NUMBER) % (TILE_NUMBER * TILE_NUMBER);
        int innerByteIndex = 2 * (index % TILE_PIXEL_LENGTH);
        int rangeChooser =   lcdRegFile.testBit(LcdReg.LCDC, areaRegister) ? 1 : 0;
 
        int msb;
        int lsb;
        int tileIndex;
        int tileSourceRange;
        int lsbAddress; 
   
        for (int i = 0; i < TILE_NUMBER; i++) {
             tileIndex = read(AddressMap.BG_DISPLAY_DATA[rangeChooser] + outerTileIndex + i);    
             tileSourceRange = 1;
            
            if(!lcdRegFile.testBit(LcdReg.LCDC, LCDCReg.TILE_SOURCE)){
                tileSourceRange = 0; // to fetch from first range
                tileIndex = Bits.clip(Byte.SIZE, tileIndex + OVERFLOW_8BITS); //to change the order of tile numbering
            }
                lsbAddress = AddressMap.TILE_SOURCE[tileSourceRange] + MSB_AND_LSB_PIXELS * tileIndex + innerByteIndex;            
                lsb = read(lsbAddress);
                msb = read(lsbAddress + 1);              
                imageBuilder.setBytes(i, reverse8(msb), reverse8(lsb));
        }
        return imageBuilder.build();
            
    }  
    
    /**
     * @param lineIndex : sprite line we want to compute to finish our line
     * @param behind_BG : determines whether we are computing background sprites
     * or foreground sprites
     * @return all the superpositioned individual sprite lines by putting them underneath
     * in order (with below) 
     */
    private LcdImageLine getSpriteLine(int lineIndex, boolean behind_BG) {
        checkArgument(lineIndex >= 0 && lineIndex < WINDOW_LENGTH);
        
        int[] spriteIndexArray = spritesIntersectingLine(lineIndex);
        LcdImageLine spriteLine = new LcdImageLine.Builder(LCD_WIDTH).build();
        LcdImageLine individualSpriteLine;
        int spriteCaracteristics;
        int address;
        for (int i = 0; i < spriteIndexArray.length; ++i) {
            address = AddressMap.OAM_START + spriteIndexArray[i] * SPRITE_FIELD_NUMBER
                    + SpriteInformation.CHARACTERISTICS.index();
            spriteCaracteristics = read(address);
            if (test(spriteCaracteristics, SpriteByteCharacteristics.BEHIND_BG.index()) == behind_BG) {
                individualSpriteLine = getIndividualSpriteLine(lineIndex, spriteIndexArray[i]);
                spriteLine = LcdImageLine.below(spriteLine, individualSpriteLine);
            }
        }
        return spriteLine;
    }

    /**
     * @param lineIndex
     * @param spriteIndex , index of sprite which intersects with line 
     * @return a line in which we can find the indexed sprite,
     * using this method multiple time in getSpriteLine we for a line of sprites
     */
    private LcdImageLine getIndividualSpriteLine(int lineIndex, int spriteIndex) {

        LcdImageLine.Builder imageBuilder = new LcdImageLine.Builder(LCD_WIDTH);
        int palette;
       
        palette = test(read(AddressMap.OAM_START + spriteIndex * SPRITE_FIELD_NUMBER 
                + SPRITE_CHARACTERISTICS_BYTE), SpriteByteCharacteristics.PALETTE.index()) ? 
                        getReg(LcdReg.OBP1) : getReg(LcdReg.OBP0);
        
        int coorX = read(AddressMap.OAM_START + spriteIndex * SPRITE_FIELD_NUMBER + 
                SpriteInformation.COOR_X.index()) - COOR_X_ADJUSTER;
        int indexTile = read(AddressMap.OAM_START + spriteIndex * SPRITE_FIELD_NUMBER + 
                SpriteInformation.TILE_INDEX.index());
        int lsbAddress = AddressMap.TILE_SOURCE[1] + indexTile * MSB_AND_LSB_PIXELS +
                (lineIndex - read(AddressMap.OAM_START + spriteIndex * SPRITE_FIELD_NUMBER) 
                        + MSB_AND_LSB_PIXELS) * 2;


        //FLIP_V
        if(test(read(AddressMap.OAM_START + spriteIndex * SPRITE_FIELD_NUMBER 
                + SPRITE_CHARACTERISTICS_BYTE), SpriteByteCharacteristics.FLIP_V.index())){ 
           
           // checking if it's an 8x8 or 8x16 sprite, and doing necessary alterations to change the order of taken bytes 
           lsbAddress = lcdRegFile.testBit(LcdReg.LCDC, LCDCReg.OBJ_SIZE) ? 
                    AddressMap.TILE_SOURCE[1] + indexTile * MSB_AND_LSB_PIXELS
                            + ((BIG_SPRITE_INVERTER - (lineIndex - read(AddressMap.OAM_START + spriteIndex * SPRITE_FIELD_NUMBER) 
                                    + MSB_AND_LSB_PIXELS)) * 2)
                    : AddressMap.TILE_SOURCE[1] + indexTile * MSB_AND_LSB_PIXELS 
                            + ((SMALL_SPRITE_INVERTER - (lineIndex - read(AddressMap.OAM_START + spriteIndex * SPRITE_FIELD_NUMBER) 
                                    + MSB_AND_LSB_PIXELS)) * 2);
        }
 
        int lsb = read(lsbAddress);
        int msb = read(lsbAddress + 1);
        LcdImageLine spriteLine; 
        
        boolean horizontalFlipCondition = Bits.test(
                read(AddressMap.OAM_START + (spriteIndex * SPRITE_FIELD_NUMBER)
                        + SPRITE_CHARACTERISTICS_BYTE),
                SpriteByteCharacteristics.FLIP_H.index());

        //FLIP_H 
        spriteLine = horizontalFlipCondition ? imageBuilder.setBytes(0, msb, lsb).build() :
            imageBuilder.setBytes(0, reverse8(msb), reverse8(lsb)).build();

        return spriteLine.shift(coorX).mapColors(palette);

    }

    /**
     * @param index of line we want to compute
     * @return an array of sprite indexes which happens to 
     * intersect with line we would like to draw
     * to do so we fetch the Y coordinates of sprites, takes whoever 
     * crosses with that line(index) then according to the method proposed,
     * we sort them and return in order to use it in getSpriteLine
     */
    private int[] spritesIntersectingLine(int index) {      
        int[] spritesFound = new int[MAX_SPRITE_IN_LINE];
        int[] sortedSpriteArray;
        int spriteCount = 0;
        int coorX;
        int coorY;  
        int spriteIndex = 0;
        boolean sizeCondition;
        
        while(spriteCount < spritesFound.length  && spriteIndex <  MAX_SPRITE_NUMBER ) {
            coorY = read(AddressMap.OAM_START + spriteIndex * SPRITE_FIELD_NUMBER) - COOR_Y_ADJUSTER;        
           
            sizeCondition = coorY <= index && index < coorY + SMALL_SPRITE_PIXEL_HEIGHT;
            
            if(lcdRegFile.testBit(LcdReg.LCDC, LCDCReg.OBJ_SIZE)){
                sizeCondition = coorY <= index && index < coorY + BIG_SPRITE_PIXEL_HEIGHT;
            }

            if(sizeCondition) {
                coorX = read(AddressMap.OAM_START + spriteIndex * SPRITE_FIELD_NUMBER + 1);
                spritesFound[spriteCount] = Bits.make16(coorX, spriteIndex);
                ++spriteCount;
            }
            ++spriteIndex;
        }
        Arrays.sort(spritesFound, 0, spriteCount);
        sortedSpriteArray = new int[spriteCount];
        
        for (spriteIndex = 0; spriteIndex < spriteCount; spriteIndex++) {
            sortedSpriteArray[spriteIndex] = Bits.clip(Byte.SIZE, spritesFound[spriteIndex]);
        }
        
        return  sortedSpriteArray;        
    }

    /**
     * @param adjustedWX 
     * @returns if the window should be shown or not by
     * checking the WIN bit and checking if adjustedWX is in proper limits(on screen)
     */
    private boolean windowEnabled(int adjustedWX) {
        return lcdRegFile.testBit(LcdReg.LCDC, LCDCReg.WIN) && adjustedWX >= 0 && adjustedWX < LCD_WIDTH;
    }

    /**
     * @param reg : Lcd Register to get data
     * @return data on register
     * method for writing a shorter, cleaner code
     */
    private int getReg(LcdReg reg) {
        return lcdRegFile.get(reg);
    }
    
    /**
     * @param reg : Lcd Register to set data
     * @param data : value to be set
     * method for writing a shorter, cleaner code 
     */
    private void setReg(LcdReg reg, int data) {
        lcdRegFile.set(reg, data);
    }
   

    /**
     * @param inLineCycleNumber : to determine whether we are in vblank or not
     * @param lineNumber : to determine which mode we are in when we are not in vblank
     * @return the number of corresponding mode
     */
    private Modes getMode(int inLineCycleNumber, int lineNumber) {

        if (lineNumber >= 0 && lineNumber < LCD_HEIGHT) {
            if (0 <= inLineCycleNumber && inLineCycleNumber < CYCLES_IN_MODE_2) {
                return Modes.MODE_2;
            }
            if (CYCLES_IN_MODE_2 <= inLineCycleNumber && inLineCycleNumber < CYCLES_IN_MODE_2 + CYCLES_IN_MODE_3) {

                return Modes.MODE_3;
            }
            return Modes.MODE_0;
        }
        return Modes.MODE_1;
    }
    
    /**
     * @param mode : mode we want to set for bits MODE0 and MODE1 of STAT register
     */
    private void setMode(Modes wantedMode) {
        int mode = wantedMode.ordinal();
        boolean mode1_flag = Bits.test(mode, 1);
        boolean mode0_flag = Bits.test(mode, 0);
        lcdRegFile.setBit(LcdReg.STAT, STATReg.MODE1, mode1_flag);
        lcdRegFile.setBit(LcdReg.STAT, STATReg.MODE0, mode0_flag);
    }

    /**
     * @param newValue : value to be set on Lcd Register LY
     * also calls the control method for interruptions in case of an LY, LYC equality
     */
    private void modifyLY(int newValue) {
        setReg(LcdReg.LY, newValue);
        modifyLYC_EQ_LY();
    }
    
    /**
     * @param newValue : value to be set on Lcd Register LYC
     * also calls the control method for interruptions in case of an LY, LYC equality
     */
    private void modifyLYC(int newValue) {
        setReg(LcdReg.LYC, newValue);
        modifyLYC_EQ_LY();
    }
    
    /**
     * this method being called at every change on LY or LYC, checks if they are equal,
     * in the case of equality sets the bit LYC_EQ_LY of STAT and requests interruption
     * with an additional control of bit INT_LYC of STAT Register
     */
    private void modifyLYC_EQ_LY() {
        boolean LYC_equals_LY = getReg(LcdReg.LY) == getReg(LcdReg.LYC);
        lcdRegFile.setBit(LcdReg.STAT, STATReg.LYC_EQ_LY,  LYC_equals_LY);
        
        // interruption
        if (LYC_equals_LY && lcdRegFile.testBit(LcdReg.STAT, STATReg.INT_LYC)) {
           gameboyCpu.requestInterrupt(Interrupt.LCD_STAT);
        }
        
    }

    /* (non-Javadoc)
     * @see ch.epfl.gameboj.component.Component#attachTo(ch.epfl.gameboj.Bus)
     * this method lets us reach the bus, so we can read from proper address to get
     * the information of sprites for fast copy process
     */
    @Override
    public void attachTo(Bus bus) {
        Objects.requireNonNull(bus);
        this.bus = bus;
        Component.super.attachTo(bus);

    }
    
}