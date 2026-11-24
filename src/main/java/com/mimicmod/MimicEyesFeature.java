package com.mimicmod;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.util.Identifier;

/** Светящиеся в темноте глаза и зубы. */
public class MimicEyesFeature extends EyesFeatureRenderer<MimicEntity, MimicModel> {
	private static final RenderLayer EYES = RenderLayer.getEyes(new Identifier(MimicMod.MOD_ID, "textures/entity/mimic_eyes.png"));

	public MimicEyesFeature(FeatureRendererContext<MimicEntity, MimicModel> context) {
		super(context);
	}

	@Override
	public RenderLayer getEyesTexture() {
		return EYES;
	}
}
