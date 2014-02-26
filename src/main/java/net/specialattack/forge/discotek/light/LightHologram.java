
package net.specialattack.forge.discotek.light;

import java.util.Arrays;
import java.util.List;

import net.specialattack.forge.discotek.block.BlockLight;

public class LightHologram implements ILight {

    private final List<Channels> channels;

    public LightHologram() {
        this.channels = Arrays.asList(Channels.BRIGHTNESS, Channels.TILT, Channels.PAN, Channels.SIZE, Channels.RED, Channels.GREEN, Channels.BLUE, Channels.NAME);
    }

    @Override
    public List<Channels> getChannels() {
        return this.channels;
    }

    @Override
    public boolean supportsLens() {
        return false;
    }

    @Override
    public int getRedstonePower(int channelValue) {
        return 0;
    }

    @Override
    public String getIdentifier() {
        return "hologram";
    }

    @Override
    public void setBlockBounds(BlockLight block, int direction) {
        switch (direction) {
        case 0:
            //block.setBlockBounds(0.125F, 0.5625F, 0.125F, 0.875F, 1.0F, 0.875F);
        break;
        case 1:
            //block.setBlockBounds(0.125F, 0.0F, 0.125F, 0.875F, 0.4375F, 0.875F);
        break;
        case 2:
            //block.setBlockBounds(0.125F, 0.125F, 0.5625F, 0.875F, 0.875F, 1.0F);
        break;
        case 3:
            //block.setBlockBounds(0.125F, 0.125F, 0.0F, 0.875F, 0.875F, 0.4375F);
        break;
        case 4:
            //block.setBlockBounds(0.5625F, 0.125F, 0.125F, 1.0F, 0.875F, 0.875F);
        break;
        case 5:
            //block.setBlockBounds(0.0F, 0.125F, 0.125F, 0.4375F, 0.875F, 0.875F);
        break;
        }
        block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }

}