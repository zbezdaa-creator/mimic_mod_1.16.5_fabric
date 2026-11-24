package com.mimicmod;

import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class MimicSounds {
	public static SoundEvent WATERPHONE, DRONE, FAR_SCREAM, VANISH, FLICKER, JUMPSCARE, SEE_YOU, RUSTLE, FOOTSTEP, DIG;

	/** Длина длинных треков в тиках (20 тиков = 1 сек), чтобы не включать их одновременно. */
	public static final int WATERPHONE_TICKS = 1530;
	public static final int DRONE_TICKS = 1505;

	private static SoundEvent reg(String name) {
		Identifier id = new Identifier(MimicMod.MOD_ID, name);
		return Registry.register(Registry.SOUND_EVENT, id, new SoundEvent(id));
	}

	public static void register() {
		WATERPHONE = reg("mimic_waterphone");
		DRONE = reg("mimic_drone");
		FAR_SCREAM = reg("mimic_far_scream");
		VANISH = reg("mimic_vanish");
		FLICKER = reg("mimic_flicker");
		JUMPSCARE = reg("mimic_jumpscare");
		SEE_YOU = reg("mimic_see_you");
		RUSTLE = reg("mimic_rustle");
		FOOTSTEP = reg("mimic_footstep");
		DIG = reg("mimic_dig");
	}
}
