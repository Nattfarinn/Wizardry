package com.teamwizardry.wizardry.common.module.effects;

import com.teamwizardry.wizardry.api.spell.IDelayedModule;
import com.teamwizardry.wizardry.api.spell.SpellData;
import com.teamwizardry.wizardry.api.spell.SpellRing;
import com.teamwizardry.wizardry.api.spell.attribute.AttributeRegistry;
import com.teamwizardry.wizardry.api.spell.module.ModuleEffect;
import com.teamwizardry.wizardry.api.spell.module.ModuleModifier;
import com.teamwizardry.wizardry.api.util.RandUtil;
import com.teamwizardry.wizardry.client.fx.LibParticles;
import com.teamwizardry.wizardry.common.module.modifiers.ModuleModifierIncreaseDuration;
import com.teamwizardry.wizardry.init.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.awt.*;

/**
 * Created by Demoniaque.
 */
//TODO: @RegisterModule
public class ModuleEffectTimeLock extends ModuleEffect implements IDelayedModule {

	@Nonnull
	@Override
	public String getID() {
		return "effect_time_lock";
	}

	@Override
	public ModuleModifier[] applicableModifiers() {
		return new ModuleModifier[]{new ModuleModifierIncreaseDuration()};
	}

	@Override
	public boolean run(@Nonnull SpellData spell, @Nonnull SpellRing spellRing) {
		World world = spell.world;
		Entity targetEntity = spell.getVictim();

		double time = spellRing.getAttributeValue(AttributeRegistry.DURATION, spell);

		if (!spellRing.taxCaster(spell)) return false;

		if (targetEntity != null) {
			targetEntity.setFire((int) time);
			world.playSound(null, targetEntity.getPosition(), ModSounds.FIRE, SoundCategory.NEUTRAL, RandUtil.nextFloat(0.35f, 0.75f), RandUtil.nextFloat(0.35f, 1.5f));
		}
		return true;
	}

	@Override
	public void runDelayedEffect(SpellData spell, SpellRing spellRing) {

	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(@Nonnull SpellData spell, @Nonnull SpellRing spellRing) {
		World world = spell.world;
		Vec3d position = spell.getTarget();

		if (position == null) return;

		Color color = getPrimaryColor();
		if (RandUtil.nextBoolean()) color = getSecondaryColor();

		LibParticles.EFFECT_BURN(world, position, color);
	}
}