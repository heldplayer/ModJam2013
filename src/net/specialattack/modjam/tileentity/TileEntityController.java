
package net.specialattack.modjam.tileentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.specialattack.modjam.Instruction;

public class TileEntityController extends TileEntity {

    private List<ChunkCoordinates> lightsLinked = new ArrayList<ChunkCoordinates>();
    public int[] levels = new int[512];
    public int[] stack = new int[16];
    public int stackPointer;
    public boolean interpretFirst;
    public Instruction[] instructions;
    public int instructionPointer;
    public String error;
    public int errorIndex;
    private boolean running; // Do NOT turn me on yet!

    public TileEntityController() {
        for (int i = 0; i < this.levels.length; i++) {
            this.levels[i] = 0x0;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        NBTTagList lightsLinked = compound.getTagList("Lights");
        for (int i = 0; i < lightsLinked.tagCount(); i++) {
            NBTTagCompound tag = (NBTTagCompound) lightsLinked.tagAt(i);
            ChunkCoordinates coord = new ChunkCoordinates();
            coord.posX = tag.getInteger("x");
            coord.posY = tag.getInteger("y");
            coord.posZ = tag.getInteger("z");
            this.link(coord);
        }
        this.levels = compound.getIntArray("Levels");
        if (this.levels.length < 512) {
            int[] temp = this.levels;
            this.levels = new int[512];
            System.arraycopy(temp, 0, this.levels, 0, temp.length);
        }
        this.instructionPointer = compound.getInteger("Pointer");
        NBTTagList instructions = compound.getTagList("Instructions");
        this.instructions = new Instruction[instructions.tagCount()];
        for (int i = 0; i < instructions.tagCount(); i++) {
            NBTTagCompound tag = (NBTTagCompound) instructions.tagAt(i);
            Instruction instruction = new Instruction();
            instruction.identifier = tag.getString("Identifier");
            instruction.argument = tag.getInteger("Argument");
            if (!instruction.identifier.equals("NOOP")) {
                this.instructions[i] = instruction;
            }
        }
        this.stack = compound.getIntArray("Stack");
        this.stackPointer = compound.getInteger("StackPointer");
        this.interpretFirst = compound.getBoolean("Interpret");
        this.running = compound.getBoolean("Running");
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        NBTTagList lightsLinked = new NBTTagList();
        for (ChunkCoordinates coord : this.lightsLinked) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("x", coord.posX);
            tag.setInteger("y", coord.posY);
            tag.setInteger("z", coord.posZ);
            lightsLinked.appendTag(tag);
        }
        compound.setTag("Lights", lightsLinked);
        compound.setIntArray("Levels", this.levels);
        compound.setInteger("Pointer", this.instructionPointer);
        NBTTagList instructions = new NBTTagList();
        for (Instruction instruction : this.instructions) {
            if (instruction == null) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setString("Identifier", "NOOP");
                tag.setInteger("Argument", 0);
                instructions.appendTag(tag);
                continue;
            }
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Identifier", instruction.identifier);
            tag.setInteger("Argument", instruction.argument);
            instructions.appendTag(tag);
        }
        compound.setTag("Instructions", instructions);
        compound.setIntArray("Stack", this.stack);
        compound.setInteger("StackPointer", this.stackPointer);
        compound.setBoolean("InterpretFirst", this.interpretFirst);
        compound.setBoolean("Running", this.running);
    }

    public boolean link(ChunkCoordinates coords) {
        if (this.lightsLinked.contains(coords)) {
            return false;
        }

        double reach = 32.0D * 32.0D;
        double distance = (coords.posX - this.xCoord) * (coords.posX - this.xCoord);
        distance += (coords.posY - this.yCoord) * (coords.posY - this.yCoord);
        distance += (coords.posZ - this.zCoord) * (coords.posZ - this.zCoord);

        if (distance < reach) {
            this.lightsLinked.add(coords);

            return true;
        }

        return false;
    }

    public void setChannelLevel(int channel, int percent) {
        if (channel >= 0 && channel < 256) {
            float oldPercent = this.levels[channel];
            if (oldPercent != percent) {
                this.levels[channel] = percent;
                this.updateDmxNetwork();
            }
        }
    }

    public void updateDmxNetwork() {
        for (int i = 0; i < this.lightsLinked.size(); i++) {
            ChunkCoordinates coord = this.lightsLinked.get(i);
            TileEntity tile = this.worldObj.getBlockTileEntity(coord.posX, coord.posY, coord.posZ);
            if (tile != null && tile instanceof TileEntityLight) {
                ((TileEntityLight) tile).sendUniverseData(this.levels);
            }
            else {
                this.lightsLinked.remove(i);
                i--;
            }
        }
    }

    @Override
    public void updateEntity() {
        if (!this.worldObj.isRemote) {
            int size = 0;
            switch (this.getBlockMetadata()) {
            case 1:
                size = 50;
            break;
            }
            if (this.instructions == null) {
                this.instructions = new Instruction[size];
            }
            else if (this.instructions.length != size) {
                Instruction[] temp = this.instructions;
                this.instructions = new Instruction[size];
                System.arraycopy(temp, 0, this.instructions, 0, temp.length < this.instructions.length ? temp.length : this.instructions.length);
            }

            try {
                if (this.running && this.instructions[this.instructionPointer] != null) {
                    Instruction instruction = this.instructions[this.instructionPointer];

                    if (instruction.identifier.equals("SLEEP")) { // Sleep for N ticks
                        if (this.interpretFirst) {
                            this.interpretFirst = false;
                            this.pushStack(instruction.argument);
                        }
                        else {
                            int value = this.popStack();
                            value--;
                            if (value == 0) {
                                this.next();
                            }
                            else {
                                this.pushStack(value);
                            }
                        }
                    }
                    else if (instruction.identifier.equals("PUSH")) { // Push N to the stack
                        this.pushStack(instruction.argument);
                        this.next();
                    }
                    else if (instruction.identifier.equals("POP")) { // Pop the stack
                        this.popStack();
                        this.next();
                    }
                    else if (instruction.identifier.equals("LEV")) { // Set channel to N
                        int channel = this.popStack();
                        int value = instruction.argument;
                        this.setChannelLevel(channel, value);
                        this.next();
                    }
                    else if (instruction.identifier.equals("LEV2")) { // Set 2 channels
                        int value2 = instruction.argument;
                        int channel2 = this.popStack();
                        int value1 = this.popStack();
                        int channel1 = this.popStack();
                        this.setChannelLevel(channel1, value1);
                        this.setChannelLevel(channel2, value2);
                        this.next();
                    }
                    else if (instruction.identifier.equals("LEV3")) { // Set 3 channels
                        int value3 = instruction.argument;
                        int channel3 = this.popStack();
                        int value2 = this.popStack();
                        int channel2 = this.popStack();
                        int value1 = this.popStack();
                        int channel1 = this.popStack();
                        this.setChannelLevel(channel1, value1);
                        this.setChannelLevel(channel2, value2);
                        this.setChannelLevel(channel3, value3);
                        this.next();
                    }
                    else if (instruction.identifier.equals("GOTO")) { // Go to instruction at index N
                        this.changeTo(instruction.argument - 1);
                    }
                    else if (instruction.identifier.equals("CLEAR")) { // Clear the stack
                        this.stackPointer = 0;
                        this.next();
                    }
                    else {
                        throw new ControllerException("gui.controller.unknowninstruction", this.instructionPointer + 1);
                    }
                }
                else {
                    this.next();
                }
                this.onInventoryChanged();
            }
            catch (ControllerException e) {
                this.running = false;
                this.error = e.getMessage();
                this.errorIndex = e.index;
                e.printStackTrace();
            }
        }
    }

    private void changeTo(int index) {
        this.instructionPointer = index;
        if (this.instructionPointer < 0 || this.instructionPointer >= this.instructions.length) {
            this.instructionPointer = 0;
        }
        this.interpretFirst = true;
    }

    private void next() {
        this.instructionPointer++;
        if (this.instructionPointer >= this.instructions.length) {
            this.instructionPointer = 0;
        }
        this.interpretFirst = true;
    }

    private void pushStack(int value) throws ControllerException {
        this.stackPointer++;
        if (this.stackPointer >= this.stack.length) {
            throw new ControllerException("gui.controller.stackoverflow", this.instructionPointer);
        }
        this.stack[this.stackPointer] = value;
    }

    private int popStack() throws ControllerException {
        this.stackPointer--;
        if (this.stackPointer < 0) {
            throw new ControllerException("gui.controller.stackunderflow", this.instructionPointer);
        }
        return this.stack[this.stackPointer + 1];
    }

    public void startStop() {
        this.running = !this.running;
        if (this.running) {
            this.interpretFirst = true;
            this.stackPointer = 0;
            this.stack = new int[16];
            this.instructionPointer = 0;
            this.error = null;
            this.errorIndex = 0;
        }
    }

    public static class ControllerException extends Exception {

        private static final long serialVersionUID = 7486507998690757872L;
        public final int index;

        public ControllerException(String message, int index) {
            super(message);
            this.index = index;
        }

    }

}
