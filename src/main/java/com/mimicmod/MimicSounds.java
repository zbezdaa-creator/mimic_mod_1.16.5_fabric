package com.mimicmod;

import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class MimicSounds {
	public static SoundEvent HI, SEE_YOU, NOT_ME, DONT_KILL, SCREAM, CRY, RUSTLE, FOOTSTEP, DIG;

	private static SoundEvent reg(String name) {
		Identifier id = new Identifier(MimicMod.MOD_ID, name);
		return Registry.register(Registry.SOUND_EVENT, id, new SoundEvent(id));
	}

	public static void register() {
		HI = reg("mimic_hi");
		SEE_YOU = reg("mimic_see_you");
		NOT_ME = reg("mimic_not_me");
		DONT_KILL = reg("mimic_dont_kill");
		SCREAM = reg("mimic_scream");
		CRY = reg("mimic_cry");
		RUSTLE = reg("mimic_rustle");
		FOOTSTEP = reg("mimic_footstep");
		DIG = reg("mimic_dig");
	}
}
