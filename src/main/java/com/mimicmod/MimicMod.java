package com.mimicmod;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class MimicMod implements ModInitializer {
	public static final String MOD_ID = "mimicmod";
	public static EntityType<MimicEntity> MIMIC;

	@Override
	public void onInitialize() {
		MimicSounds.register();

		MIMIC = Registry.register(Registry.ENTITY_TYPE, new Identifier(MOD_ID, "mimic"),
				FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, MimicEntity::new)
						.dimensions(EntityDimensions.fixed(0.7F, 2.6F))
						.trackRangeBlocks(128)
						.build());
		FabricDefaultAttributeRegistry.register(MIMIC, MimicEntity.createMimicAttributes());

		Registry.register(Registry.ITEM, new Identifier(MOD_ID, "mimic_spawn_egg"),
				new SpawnEggItem(MIMIC, 0x101010, 0xE8E8E0, new Item.Settings().group(ItemGroup.MISC)));

		MimicDirector.init();

		// Команды для теста (нужны права оператора / читы)
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(
				CommandManager.literal("mimic")
						.requires(src -> src.hasPermissionLevel(2))
						.then(CommandManager.literal("spawn").executes(c -> {
							ServerPlayerEntity p = c.getSource().getPlayer();
							MimicDirector.appear(p, MimicDirector.phase(c.getSource().getMinecraftServer()) == 2);
							return 1;
						}))
						.then(CommandManager.literal("scare").executes(c -> {
							MimicDirector.randomEvent(c.getSource().getPlayer());
							return 1;
						}))
						.then(CommandManager.literal("phase")
								.then(CommandManager.literal("1").executes(c -> setPhase(c, 1)))
								.then(CommandManager.literal("2").executes(c -> setPhase(c, 2)))
								.then(CommandManager.literal("auto").executes(c -> setPhase(c, 0))))));
	}

	private static int setPhase(CommandContext<ServerCommandSource> c, int phase) {
		MimicDirector.forcedPhase = phase;
		c.getSource().sendFeedback(new LiteralText("Mimic phase: " + (phase == 0 ? "auto" : phase)), false);
		return 1;
	}
}
