package com.teamwizardry.wizardry.client.core;

import java.util.List;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

import org.lwjgl.opengl.GL11;

import com.teamwizardry.librarianlib.cloth.Cloth;
import com.teamwizardry.librarianlib.cloth.Link;
import com.teamwizardry.librarianlib.cloth.PointMass;
import com.teamwizardry.librarianlib.math.Matrix4;

public class CapeHandler {

	public static final CapeHandler INSTANCE = new CapeHandler();
	public Cloth c;
	
	WeakHashMap<EntityLivingBase, Cloth> cloths = new WeakHashMap<>();
	
	private CapeHandler() {
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@SubscribeEvent
	public void tick(ClientTickEvent event) {
//		Matrix4 matrix = new Matrix4();
//		matrix.rotate(Math.toRadians(1), new Vec3d(0, 1, 0));
//		for (PointMass[] column : c.masses) {
//			for (PointMass point : column) {
//				if(!point.pin)
//					continue;
//				point.pos = matrix.apply(point.pos);
//			}
//		}
		
		for (Entry<EntityLivingBase, Cloth> e : cloths.entrySet()) {
			EntityLivingBase entity = e.getKey();
			List<AxisAlignedBB> aabbs = entity.worldObj.getCollisionBoxes(entity.getEntityBoundingBox().expand(5, 5, 5));
			e.getValue().tick(aabbs);
		}
		
//		c.tick();
	}
	
	@SubscribeEvent
	public void damage(ItemTossEvent event) {
		if(event.getEntityItem().getEntityItem().getItem() == Items.PORKCHOP)
			c.init();
		if(event.getEntityItem().getEntityItem().getItem() == Items.BEEF) {
			for (PointMass[] column : c.masses) {
				for (PointMass point : column) {
					if(!point.pin)
						continue;
					point.pos = point.pos.add(new Vec3d(0.25, 0, 0.25));
				}
			}
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@SubscribeEvent
	public void drawPlayer(RenderLivingEvent.Post event) {
		if(!( event.getEntity() instanceof EntityVillager || event.getEntity() instanceof EntityPlayer))
			return;
		
		Vec3d[] shoulderPoints = new Vec3d[] {
				new Vec3d( 0.25, 1.5,  0),
				new Vec3d( 0.25, 1.5, -0.25),
				new Vec3d( 0,    1.5, -0.25),
				new Vec3d(-0.25, 1.5, -0.25),
				new Vec3d(-0.25, 1.5,  0)
		};
		
		Matrix4 matrix = new Matrix4();
		matrix.translate(event.getEntity().getPositionVector());
		matrix.rotate(Math.toRadians( event.getEntity().rotationYawHead), new Vec3d(0, -1, 0));
		
		for (int i = 0; i < shoulderPoints.length; i++) {
			shoulderPoints[i] = matrix.apply(shoulderPoints[i]);
		}
		
		if(!cloths.containsKey(event.getEntity())) {
			cloths.put(event.getEntity(), new Cloth(
					shoulderPoints,
					10,
					new Vec3d(0, 0.2, 0)
			) );
		}
		Cloth c = cloths.get(event.getEntity());
		
		for (int i = 0; i < c.masses[0].length && i < shoulderPoints.length; i++) {
			c.masses[0][i].pos = shoulderPoints[i];
		}
		
		
		Tessellator tess = Tessellator.getInstance();
		VertexBuffer vb = tess.getBuffer();
		
		GlStateManager.pushAttrib();
		GlStateManager.pushMatrix();
		GlStateManager.translate(event.getX(), event.getY(), event.getZ());
		GlStateManager.translate(-event.getEntity().posX, -event.getEntity().posY, -event.getEntity().posZ);
		GlStateManager.color(0, 1, 0);
		GlStateManager.disableLighting();
		GlStateManager.disableTexture2D();
		
		vb.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
		for (Link link : c.links) {
			vb.pos(
					link.a.prevPos.xCoord, 
					link.a.prevPos.yCoord, 
					link.a.prevPos.zCoord).endVertex();
			vb.pos(link.b.pos.xCoord, link.b.pos.yCoord, link.b.pos.zCoord).endVertex();
		}
		tess.draw();
		
		GlStateManager.popMatrix();
		GlStateManager.popAttrib();
	}
}
