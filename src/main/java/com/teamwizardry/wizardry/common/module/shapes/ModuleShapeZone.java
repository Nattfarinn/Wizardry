package com.teamwizardry.wizardry.common.module.shapes;

import static com.teamwizardry.wizardry.api.spell.SpellData.DefaultKeys.CASTER;
import static com.teamwizardry.wizardry.api.spell.SpellData.DefaultKeys.ORIGIN;
import static com.teamwizardry.wizardry.api.spell.SpellData.DefaultKeys.PITCH;
import static com.teamwizardry.wizardry.api.spell.SpellData.DefaultKeys.SEED;
import static com.teamwizardry.wizardry.api.spell.SpellData.DefaultKeys.TARGET_HIT;
import static com.teamwizardry.wizardry.api.spell.SpellData.DefaultKeys.YAW;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.teamwizardry.librarianlib.features.math.interpolate.position.InterpCircle;
import com.teamwizardry.librarianlib.features.particle.ParticleBuilder;
import com.teamwizardry.librarianlib.features.particle.ParticleSpawner;
import com.teamwizardry.librarianlib.features.particle.functions.InterpFadeInOut;
import com.teamwizardry.wizardry.Wizardry;
import com.teamwizardry.wizardry.api.Constants;
import com.teamwizardry.wizardry.api.spell.ILingeringModule;
import com.teamwizardry.wizardry.api.spell.SpellData;
import com.teamwizardry.wizardry.api.spell.attribute.Attributes;
import com.teamwizardry.wizardry.api.spell.module.Module;
import com.teamwizardry.wizardry.api.spell.module.ModuleModifier;
import com.teamwizardry.wizardry.api.spell.module.ModuleShape;
import com.teamwizardry.wizardry.api.spell.module.RegisterModule;
import com.teamwizardry.wizardry.api.util.RandUtil;
import com.teamwizardry.wizardry.api.util.RandUtilSeed;
import com.teamwizardry.wizardry.api.util.interp.InterpScale;
import com.teamwizardry.wizardry.common.module.modifiers.ModuleModifierExtendRange;
import com.teamwizardry.wizardry.common.module.modifiers.ModuleModifierExtendTime;
import com.teamwizardry.wizardry.common.module.modifiers.ModuleModifierIncreaseAOE;
import com.teamwizardry.wizardry.common.module.modifiers.ModuleModifierIncreasePotency;

import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * Created by LordSaad.
 */
@RegisterModule
public class ModuleShapeZone extends ModuleShape implements ILingeringModule {

	@Nonnull
	@Override
	public String getID() {
		return "shape_zone";
	}

	@Nonnull
	@Override
	public String getReadableName() {
		return "Zone";
	}

	@Nonnull
	@Override
	public String getDescription() {
		return "Will linger in the area targeted in a circle, continuously running a spell in that region.";
	}

	@Override
	public ModuleModifier[] applicableModifiers() {
		return new ModuleModifier[]{new ModuleModifierIncreaseAOE(), new ModuleModifierIncreasePotency(), new ModuleModifierExtendRange(), new ModuleModifierExtendTime()};
	}

	@Override
	@SuppressWarnings("unused")
	public boolean run(@Nonnull SpellData spell) {
		World world = spell.world;
		Vec3d position = spell.getData(ORIGIN);
		Entity caster = spell.getData(CASTER);
		Vec3d targetPos = spell.getData(TARGET_HIT);
		long seed = RandUtil.nextLong(100, 1000000);
		spell.addData(SEED, seed);

		RandUtilSeed r = new RandUtilSeed(seed);

		if (targetPos == null) return false;

		double aoe = getModifier(spell, Attributes.AREA, 3, 10);
		double strength = getModifier(spell, Attributes.POTENCY, 1, 10);
		double range = getModifier(spell, Attributes.RANGE, 1, 20);

		List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(null, new AxisAlignedBB(new BlockPos(targetPos)).grow(aoe, 1, aoe));

		setMultiplier(0.7);
		if (r.nextInt((int) ((70 - strength))) == 0) {
			for (Entity entity : entities) {
				if (entity.getDistance(targetPos.x, targetPos.y, targetPos.z) <= aoe) {
					Vec3d vec = targetPos.addVector(RandUtil.nextDouble(-strength, strength), RandUtil.nextDouble(range), RandUtil.nextDouble(-strength, strength));

					SpellData copy = spell.copy();
					copy.processEntity(entity, false);
					copy.addData(YAW, entity.rotationYaw);
					copy.addData(PITCH, entity.rotationPitch);
					copy.addData(ORIGIN, vec);
					runNextModule(copy);
				}
			}
		}

		if (r.nextInt((int) ((40 - strength))) != 0) return true;
		ArrayList<Vec3d> blocks = new ArrayList<>();
		for (double i = -aoe; i < aoe; i++)
			for (double j = 0; j < range; j++)
				for (double k = -aoe; k < aoe; k++) {
					Vec3d pos = targetPos.addVector(i, j, k);
					if (pos.distanceTo(targetPos) <= aoe) {
//						BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos(new BlockPos(pos));
						blocks.add(pos);
					}
				}
		if (blocks.isEmpty()) return true;
		Vec3d pos = blocks.get(RandUtil.nextInt(blocks.size() - 1));

		SpellData copy = spell.copy();
		copy.addData(ORIGIN, pos);
		copy.processBlock(new BlockPos(pos), EnumFacing.UP, pos);
		copy.addData(YAW, RandUtil.nextFloat(-180, 180));
		copy.addData(PITCH, RandUtil.nextFloat(-50, 50));
		runNextModule(copy);
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void runClient(@Nonnull SpellData spell) {
		Vec3d target = spell.getData(TARGET_HIT);

		if (target == null) return;
		if (RandUtil.nextInt(10) != 0) return;

		double aoe = getModifier(spell, Attributes.AREA, 3, 10);

		ParticleBuilder glitter = new ParticleBuilder(10);
		glitter.setRender(new ResourceLocation(Wizardry.MODID, Constants.MISC.SPARKLE_BLURRED));
		glitter.setScaleFunction(new InterpScale(1, 0));
		glitter.setCollision(true);
		ParticleSpawner.spawn(glitter, spell.world, new InterpCircle(target, new Vec3d(0, 1, 0), (float) aoe, 1, RandUtil.nextFloat()), (int) (aoe * 5), 0, (aFloat, particleBuilder) -> {
			glitter.setAlphaFunction(new InterpFadeInOut(0.3f, 0.3f));
			glitter.setLifetime(RandUtil.nextInt(10, 20));
			if (RandUtil.nextBoolean()) {
				glitter.setColor(getPrimaryColor());
			} else {
				glitter.setColor(getSecondaryColor());
			}
			glitter.addMotion(new Vec3d(
					RandUtil.nextDouble(-0.001, 0.001),
					RandUtil.nextDouble(-0.1, 0.1),
					RandUtil.nextDouble(-0.001, 0.001)
			));
		});
	}

	@Nonnull
	@Override
	public Module copy() {
		return cloneModule(new ModuleShapeZone());
	}

	@Override
	public int lingeringTime(SpellData spell) {
		double strength = getModifier(spell, Attributes.DURATION, 40, 100) * 30;
		return (int) strength;
	}
}
