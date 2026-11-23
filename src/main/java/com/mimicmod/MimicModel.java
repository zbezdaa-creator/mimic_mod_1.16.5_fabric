package com.mimicmod;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

/** Высокий, угловатый (блочный) скелетоподобный силуэт. Размер модели: 40 пикселей = 2.5 блока. */
public class MimicModel extends EntityModel<MimicEntity> {
	private final ModelPart head, body, rightArm, leftArm, rightLeg, leftLeg;

	public MimicModel() {
		this.textureWidth = 64;
		this.textureHeight = 64;

		head = new ModelPart(this, 0, 0);
		head.setPivot(0.0F, -8.0F, 0.0F);
		head.addCuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F);

		body = new ModelPart(this, 0, 16);
		body.setPivot(0.0F, -8.0F, 0.0F);
		body.addCuboid(-4.0F, 0.0F, -2.0F, 8.0F, 16.0F, 4.0F);

		rightArm = new ModelPart(this, 32, 16);
		rightArm.setPivot(-5.5F, -6.0F, 0.0F);
		rightArm.addCuboid(-1.5F, -2.0F, -1.5F, 3.0F, 16.0F, 3.0F);

		leftArm = new ModelPart(this, 44, 16);
		leftArm.setPivot(5.5F, -6.0F, 0.0F);
		leftArm.addCuboid(-1.5F, -2.0F, -1.5F, 3.0F, 16.0F, 3.0F);

		rightLeg = new ModelPart(this, 0, 40);
		rightLeg.setPivot(-2.0F, 8.0F, 0.0F);
		rightLeg.addCuboid(-1.5F, 0.0F, -1.5F, 3.0F, 16.0F, 3.0F);

		leftLeg = new ModelPart(this, 12, 40);
		leftLeg.setPivot(2.0F, 8.0F, 0.0F);
		leftLeg.addCuboid(-1.5F, 0.0F, -1.5F, 3.0F, 16.0F, 3.0F);
	}

	@Override
	public void setAngles(MimicEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
		head.yaw = headYaw * ((float) Math.PI / 180F);
		head.pitch = headPitch * ((float) Math.PI / 180F);
		head.roll = MathHelper.sin(animationProgress * 0.13F) * 0.08F;

		float swing = limbAngle * 0.6662F;
		rightLeg.pitch = MathHelper.cos(swing) * 1.4F * limbDistance;
		leftLeg.pitch = MathHelper.cos(swing + (float) Math.PI) * 1.4F * limbDistance;

		// Руки подняты к лицу (как на референсе), слегка подрагивают
		float tw = MathHelper.sin(animationProgress * 0.9F) * 0.06F;
		rightArm.pitch = -2.1F + tw;
		rightArm.yaw = -0.2F;
		rightArm.roll = 0.35F;
		leftArm.pitch = -2.2F - tw;
		leftArm.yaw = 0.2F;
		leftArm.roll = -0.35F;
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
		head.render(matrices, vertices, light, overlay, red, green, blue, alpha);
		body.render(matrices, vertices, light, overlay, red, green, blue, alpha);
		rightArm.render(matrices, vertices, light, overlay, red, green, blue, alpha);
		leftArm.render(matrices, vertices, light, overlay, red, green, blue, alpha);
		rightLeg.render(matrices, vertices, light, overlay, red, green, blue, alpha);
		leftLeg.render(matrices, vertices, light, overlay, red, green, blue, alpha);
	}
}
