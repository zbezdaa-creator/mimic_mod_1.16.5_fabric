package com.mimicmod;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

/** Искажённый "Стив": около 2.8 блока, наклонённая голова, очень длинные руки с когтями. */
public class MimicModel extends EntityModel<MimicEntity> {
	private final ModelPart head, body, rightArm, leftArm, rightLeg, leftLeg;

	public MimicModel() {
		this.textureWidth = 128;
		this.textureHeight = 64;

		head = new ModelPart(this, 0, 0);
		head.setPivot(0.0F, -12.0F, 0.0F);
		head.addCuboid(-4.5F, -9.0F, -4.5F, 9.0F, 9.0F, 9.0F);

		body = new ModelPart(this, 0, 18);
		body.setPivot(0.0F, -12.0F, 0.0F);
		body.addCuboid(-4.0F, 0.0F, -2.0F, 8.0F, 14.0F, 4.0F);

		rightArm = new ModelPart(this, 40, 0);
		rightArm.setPivot(-5.5F, -10.0F, 0.0F);
		rightArm.addCuboid(-1.5F, -2.0F, -1.5F, 3.0F, 26.0F, 3.0F);

		leftArm = new ModelPart(this, 52, 0);
		leftArm.setPivot(5.5F, -10.0F, 0.0F);
		leftArm.addCuboid(-1.5F, -2.0F, -1.5F, 3.0F, 26.0F, 3.0F);

		// длинные чёрные когти (по 3 на руку)
		for (int i = 0; i < 3; i++) {
			ModelPart r = new ModelPart(this, 96 + i * 4, 0);
			r.addCuboid(-1.5F + i, 24.0F, -0.5F, 1.0F, 6.0F, 1.0F);
			rightArm.addChild(r);
			ModelPart l = new ModelPart(this, 96 + i * 4, 0);
			l.addCuboid(-1.5F + i, 24.0F, -0.5F, 1.0F, 6.0F, 1.0F);
			leftArm.addChild(l);
		}

		rightLeg = new ModelPart(this, 64, 0);
		rightLeg.setPivot(-2.0F, 2.0F, 0.0F);
		rightLeg.addCuboid(-2.0F, 0.0F, -2.0F, 4.0F, 22.0F, 4.0F);

		leftLeg = new ModelPart(this, 80, 0);
		leftLeg.setPivot(2.0F, 2.0F, 0.0F);
		leftLeg.addCuboid(-2.0F, 0.0F, -2.0F, 4.0F, 22.0F, 4.0F);
	}

	@Override
	public void setAngles(MimicEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
		// пристальный взгляд на игрока + жуткий наклон головы
		head.yaw = headYaw * ((float) Math.PI / 180F);
		head.pitch = headPitch * ((float) Math.PI / 180F) + 0.1F;
		head.roll = 0.32F + MathHelper.sin(animationProgress * 0.05F) * 0.05F;

		float swing = limbAngle * 0.6662F;
		rightLeg.pitch = MathHelper.cos(swing) * 1.4F * limbDistance;
		leftLeg.pitch = MathHelper.cos(swing + (float) Math.PI) * 1.4F * limbDistance;

		if (entity.isAttacking()) {
			// бежит на игрока с вытянутыми длинными руками
			rightArm.pitch = -1.45F + MathHelper.sin(animationProgress * 0.6F) * 0.2F;
			leftArm.pitch = -1.45F - MathHelper.sin(animationProgress * 0.6F) * 0.2F;
			rightArm.yaw = -0.15F;
			leftArm.yaw = 0.15F;
			rightArm.roll = 0.1F;
			leftArm.roll = -0.1F;
		} else {
			// длинные руки свисают и покачиваются
			float sway = MathHelper.sin(animationProgress * 0.07F) * 0.05F;
			rightArm.pitch = MathHelper.cos(swing + (float) Math.PI) * 0.8F * limbDistance + sway;
			leftArm.pitch = MathHelper.cos(swing) * 0.8F * limbDistance - sway;
			rightArm.yaw = 0.0F;
			leftArm.yaw = 0.0F;
			rightArm.roll = 0.06F + sway;
			leftArm.roll = -0.06F - sway;
		}
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
