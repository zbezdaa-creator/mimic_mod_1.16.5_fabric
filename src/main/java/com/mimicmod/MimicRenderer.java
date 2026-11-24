package com.mimicmod;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class MimicRenderer extends MobEntityRenderer<MimicEntity, MimicModel> {
	private static final Identifier TEXTURE = new Identifier(MimicMod.MOD_ID, "textures/entity/mimic.png");

	public MimicRenderer(EntityRenderDispatcher dispatcher) {
		super(dispatcher, new MimicModel(), 0.6F);
		this.addFeature(new MimicEyesFeature(this));
	}

	@Override
	public Identifier getTexture(MimicEntity entity) {
		return TEXTURE;
	}
}
