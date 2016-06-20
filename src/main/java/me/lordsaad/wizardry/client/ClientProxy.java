package me.lordsaad.wizardry.client;

import me.lordsaad.wizardry.CommonProxy;
import me.lordsaad.wizardry.ModBlocks;
import me.lordsaad.wizardry.ModItems;
import me.lordsaad.wizardry.event.HudEventHandler;
import me.lordsaad.wizardry.event.ManaBarHandler;
import me.lordsaad.wizardry.particles.MagicBurstFX;
import me.lordsaad.wizardry.particles.ParticleRenderDispatcher;
import me.lordsaad.wizardry.particles.SparkleFX;
import me.lordsaad.wizardry.shader.ShaderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        ShaderHelper.initShaders();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        ModItems.initColors();
        ParticleRenderDispatcher.class.getName(); // load the class
        MagicBurstFX.class.getName(); // ...
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        ManaBarHandler manaBarHandler = new ManaBarHandler(Minecraft.getMinecraft());
        MinecraftForge.EVENT_BUS.register(new HudEventHandler(manaBarHandler));
    }

    @Override
    public void loadModels() {
        ModItems.initModels();
        ModBlocks.initModels();
    }

    @Override
    public void openGUI(Object gui) {
        Minecraft.getMinecraft().displayGuiScreen((GuiScreen) gui);
    }

    public SparkleFX spawnParticleSparkle(World world, double x, double y, double z, float alpha, float scale, int age) {
        SparkleFX particle = new SparkleFX(world, x, y, z, alpha, scale, age);
        Minecraft.getMinecraft().effectRenderer.addEffect(particle);
        return particle;
    }

    public SparkleFX spawnParticleSparkle(World world, double x, double y, double z, float alpha, float scale, int age, double rangeX, double rangeY, double rangeZ) {
        SparkleFX particle = new SparkleFX(world, x, y, z, alpha, scale, age, rangeX, rangeY, rangeZ);
        Minecraft.getMinecraft().effectRenderer.addEffect(particle);
        return particle;
    }

    @Override
    public MagicBurstFX spawnParticleMagicBurst(World world, double x, double y, double z) {
        MagicBurstFX particle = new MagicBurstFX(world, x, y, z);
        Minecraft.getMinecraft().effectRenderer.addEffect(particle);
        return particle;
    }
}
