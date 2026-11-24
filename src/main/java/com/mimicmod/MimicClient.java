package com.mimicmod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.client.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.Random;

@Environment(EnvType.CLIENT)
public class MimicClient implements ClientModInitializer {
	private static final Identifier JUMPSCARE_TEX = new Identifier(MimicMod.MOD_ID, "textures/gui/jumpscare.png");
	private static final Random RNG = new Random();

	private static String text = null;
	private static int textTicks = 0;
	private static int textMax = 1;
	private static int scareTicks = 0;

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.INSTANCE.register(MimicMod.MIMIC, (dispatcher, context) -> new MimicRenderer(dispatcher));

		ClientPlayNetworking.registerGlobalReceiver(MimicNet.TEXT, (client, handler, buf, responseSender) -> {
			final String s = buf.readString();
			final int ticks = buf.readInt();
			client.execute(() -> {
				text = s;
				textTicks = ticks;
				textMax = Math.max(1, ticks);
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(MimicNet.SCARE, (client, handler, buf, responseSender) -> {
			client.execute(() -> {
				scareTicks = 50;
				client.getSoundManager().play(PositionedSoundInstance.master(MimicSounds.JUMPSCARE, 1.0F, 1.0F));
			});
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (textTicks > 0) textTicks--;
			if (scareTicks > 0) scareTicks--;
		});

		HudRenderCallback.EVENT.register(MimicClient::render);
	}

	private static void render(MatrixStack matrices, float tickDelta) {
		MinecraftClient mc = MinecraftClient.getInstance();
		int w = mc.getWindow().getScaledWidth();
		int h = mc.getWindow().getScaledHeight();

		// Скример: лицо на весь экран (с тряской)
		if (scareTicks > 0) {
			int shake = scareTicks > 6 ? 6 : 0;
			int ox = shake == 0 ? 0 : RNG.nextInt(shake * 2 + 1) - shake;
			int oy = shake == 0 ? 0 : RNG.nextInt(shake * 2 + 1) - shake;
			RenderSystem.enableBlend();
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			mc.getTextureManager().bindTexture(JUMPSCARE_TEX);
			matrices.push();
			matrices.translate(ox - shake, oy - shake, 0.0D);
			matrices.scale((w + shake * 2) / 256.0F, (h + shake * 2) / 144.0F, 1.0F);
			DrawableHelper.drawTexture(matrices, 0, 0, 0.0F, 0.0F, 256, 144, 256, 144);
			matrices.pop();
		}

		// Надпись по центру экрана
		if (textTicks > 0 && text != null) {
			int alpha = Math.min(255, Math.max(8, textTicks * 255 / Math.min(textMax, 20)));
			if (textTicks > textMax - 10) alpha = 255;
			int color = (alpha << 24) | 0xB00000;
			matrices.push();
			matrices.translate(w / 2.0D + (RNG.nextInt(3) - 1), h / 2.0D - 30.0D + (RNG.nextInt(3) - 1), 0.0D);
			matrices.scale(4.0F, 4.0F, 1.0F);
			mc.textRenderer.drawWithShadow(matrices, text, -mc.textRenderer.getWidth(text) / 2.0F, 0.0F, color);
			matrices.pop();
		}
	}
}
