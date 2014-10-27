package net.specialattack.forge.discotek.packet;

import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.io.IOException;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.specialattack.forge.core.packet.Attributes;
import net.specialattack.forge.discotek.ModDiscoTek;
import net.specialattack.forge.discotek.controller.instance.ControllerPixelInstance;
import net.specialattack.forge.discotek.controller.instance.IControllerInstance;
import net.specialattack.forge.discotek.tileentity.TileEntityController;

public class Packet4PixelGui extends DiscoTekPacket {

    public int posX;
    public int posY;
    public int posZ;
    public int[] levels;

    public Packet4PixelGui() {
        super(null);
    }

    public Packet4PixelGui(ControllerPixelInstance controller) {
        super(controller.tile.getWorldObj());

        this.posX = controller.tile.xCoord;
        this.posY = controller.tile.yCoord;
        this.posZ = controller.tile.zCoord;

        this.levels = controller.levels;
    }

    @Override
    public Side getSendingSide() {
        return Side.SERVER;
    }

    @Override
    public void read(ChannelHandlerContext context, ByteBuf in) throws IOException {
        this.posX = in.readInt();
        this.posY = in.readInt();
        this.posZ = in.readInt();

        int length = in.readInt();
        this.levels = new int[length];

        for (int i = 0; i < length; i++) {
            this.levels[i] = in.readInt();
        }
    }

    @Override
    public void write(ChannelHandlerContext context, ByteBuf out) throws IOException {
        out.writeInt(this.posX);
        out.writeInt(this.posY);
        out.writeInt(this.posZ);

        out.writeInt(this.levels.length);
        for (int level : this.levels) {
            out.writeInt(level);
        }
    }

    @Override
    public void onData(ChannelHandlerContext context) {
        if (context.attr(Attributes.SENDING_PLAYER).get() == null) {
            return;
        }
        World world = context.attr(Attributes.SENDING_PLAYER).get().worldObj;

        TileEntity tile = world.getTileEntity(this.posX, this.posY, this.posZ);

        if (tile != null && tile instanceof TileEntityController) {
            TileEntityController controller = (TileEntityController) tile;
            IControllerInstance instance = controller.getControllerInstance();

            if (instance != null && instance instanceof ControllerPixelInstance) {
                ((ControllerPixelInstance) instance).levels = this.levels;
            }

            ModDiscoTek.proxy.openControllerGui(controller);
        }
    }

}
