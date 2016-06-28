package com.teamwizardry.wizardry.common.network.packets;

import com.teamwizardry.wizardry.common.item.ItemPhysicsBook;
import com.teamwizardry.wizardry.common.network.PacketBase;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketUpdateSavedPage extends PacketBase {

    String path;
    int page;

    public PacketUpdateSavedPage() {
    }

    public PacketUpdateSavedPage(String path, int page) {
        this.path = path;
        this.page = page;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        page = buf.readInt();
        path = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(page);
        ByteBufUtils.writeUTF8String(buf, path);
    }

    @Override
    public void handle(MessageContext ctx) {
        ItemPhysicsBook.setHeldPath(ctx.getServerHandler().playerEntity, path);
        ItemPhysicsBook.setHeldPage(ctx.getServerHandler().playerEntity, page);
    }

}