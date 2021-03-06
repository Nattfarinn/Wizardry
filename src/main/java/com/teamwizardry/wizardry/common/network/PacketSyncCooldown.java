package com.teamwizardry.wizardry.common.network;

import com.teamwizardry.librarianlib.features.network.PacketBase;
import com.teamwizardry.librarianlib.features.saving.Save;
import com.teamwizardry.wizardry.client.core.CooldownHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nonnull;

public class PacketSyncCooldown extends PacketBase {

	@Save
	public boolean resetMain;
	@Save
	public boolean resetOff;

	public PacketSyncCooldown() {
	}

	public PacketSyncCooldown(boolean resetMain, boolean resetOff) {
		this.resetMain = resetMain;
		this.resetOff = resetOff;
	}

	@Override
	public void handle(@Nonnull MessageContext ctx) {
		CooldownHandler.setResetMain(resetMain);
		CooldownHandler.setResetOff(resetOff);
	}
}
