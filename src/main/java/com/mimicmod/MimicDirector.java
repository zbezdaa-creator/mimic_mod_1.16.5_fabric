package com.mimicmod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

/** Управляет "сценарием": когда и что происходит рядом с каждым игроком. */
public class MimicDirector {
	/** Через сколько игровых дней (мира) начинается фаза 2. */
	public static final int PHASE2_DAY = 3;
	/** 0 = авто, 1 или 2 = принудительно (команда /mimic phase). */
	public static int forcedPhase = 0;

	private static final Random RNG = new Random();
	private static final Map<UUID, Integer> timers = new HashMap<>();
	private static final List<Task> tasks = new ArrayList<>();
	private static int now = 0;

	private static class Task {
		final int at;
		final Runnable run;
		Task(int at, Runnable run) { this.at = at; this.run = run; }
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(MimicDirector::tick);
	}

	public static int phase(MinecraftServer server) {
		if (forcedPhase != 0) return forcedPhase;
		return server.getOverworld().getTimeOfDay() / 24000L >= PHASE2_DAY ? 2 : 1;
	}

	private static void schedule(int delay, Runnable r) {
		tasks.add(new Task(now + delay, r));
	}

	private static int nextDelay(int phase) {
		// фаза 1: 40-120 сек, фаза 2: 30-90 сек
		return phase == 1 ? 800 + RNG.nextInt(1600) : 600 + RNG.nextInt(1200);
	}

	private static void tick(MinecraftServer server) {
		now++;
		for (int i = 0; i < tasks.size(); ) {
			Task t = tasks.get(i);
			if (t.at <= now) {
				tasks.remove(i);
				try { t.run.run(); } catch (Exception ignored) { }
			} else i++;
		}
		int ph = phase(server);
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			if (p.isSpectator()) continue;
			int t = timers.getOrDefault(p.getUuid(), 600 + RNG.nextInt(600)) - 1;
			if (t <= 0) {
				randomEvent(p);
				t = nextDelay(ph);
			}
			timers.put(p.getUuid(), t);
		}
	}

	// ------------------------------------------------------------ события

	public static void randomEvent(ServerPlayerEntity p) {
		int ph = phase(p.getServer());
		int r = RNG.nextInt(100);
		if (ph == 1) {
			if (r < 25) soundScare(p);
			else if (r < 40) footsteps(p);
			else if (r < 50) digging(p);
			else if (r < 62) chat(p);
			else if (r < 82) appear(p, false);
			else if (r < 92) weather(p);
			else voiceLine(p);
		} else {
			if (r < 15) soundScare(p);
			else if (r < 25) footsteps(p);
			else if (r < 35) digging(p);
			else if (r < 45) chat(p);
			else if (r < 70) appear(p, true);
			else if (r < 85) weather(p);
			else voiceLine(p);
		}
	}

	private static void soundScare(ServerPlayerEntity p) {
		SoundEvent[] pool = {MimicSounds.CRY, MimicSounds.SCREAM, MimicSounds.RUSTLE, MimicSounds.CRY};
		SoundEvent s = pool[RNG.nextInt(pool.length)];
		Vec3d pos = offset(p, 8, 25);
		playTo(p, s, pos, s == MimicSounds.RUSTLE ? 1.5F : 3.0F, 0.9F + RNG.nextFloat() * 0.2F);
	}

	private static void voiceLine(ServerPlayerEntity p) {
		SoundEvent[] pool = {MimicSounds.HI, MimicSounds.SEE_YOU, MimicSounds.NOT_ME, MimicSounds.DONT_KILL};
		playTo(p, pool[RNG.nextInt(pool.length)], offset(p, 5, 18), 2.5F, 1.0F);
	}

	private static void footsteps(ServerPlayerEntity p) {
		final double ang = RNG.nextDouble() * Math.PI * 2;
		final double start = 14 + RNG.nextInt(4);
		for (int i = 0; i < 8; i++) {
			final double d = start - i * 1.6;
			schedule(i * 9, () -> {
				Vec3d pos = new Vec3d(p.getX() - Math.sin(ang) * d, p.getY(), p.getZ() + Math.cos(ang) * d);
				playTo(p, MimicSounds.FOOTSTEP, pos, 1.6F, 0.8F + RNG.nextFloat() * 0.3F);
			});
		}
	}

	private static void digging(ServerPlayerEntity p) {
		final Vec3d pos = offset(p, 6, 16);
		for (int i = 0; i < 10; i++) {
			schedule(i * 3, () -> playTo(p, MimicSounds.DIG, pos, 1.5F, 1.0F + RNG.nextFloat() * 0.3F));
		}
	}

	private static void chat(ServerPlayerEntity p) {
		String[] lines = {"I will find you", "Hi", "I see you", "Behind you", "Don't look back", "I'm right here", "Not me..."};
		String s = lines[RNG.nextInt(lines.length)];
		if (RNG.nextInt(100) < 70) s = new StringBuilder(s).reverse().toString(); // "uoy dnif lliw I"
		p.sendMessage(new LiteralText("<Mimic> " + s), false);
	}

	private static void weather(ServerPlayerEntity p) {
		ServerWorld w = p.getServerWorld();
		if (w.getRegistryKey() != net.minecraft.world.World.OVERWORLD) { soundScare(p); return; }
		if (!w.isRaining() || !w.isThundering()) {
			w.setWeather(0, 3000 + RNG.nextInt(4000), true, true);
		}
		int strikes = 2 + RNG.nextInt(3);
		for (int i = 0; i < strikes; i++) {
			schedule(10 + RNG.nextInt(120), () -> strike(p));
		}
	}

	private static void strike(ServerPlayerEntity p) {
		ServerWorld w = p.getServerWorld();
		Vec3d o = offset(p, 15, 35);
		BlockPos bp = new BlockPos(o.x, 0, o.z);
		if (!w.isChunkLoaded(bp)) return;
		int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, bp.getX(), bp.getZ());
		LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w);
		if (bolt == null) return;
		bolt.refreshPositionAfterTeleport(o.x, y, o.z);
		bolt.setCosmetic(true); // без поджогов и урона
		w.spawnEntity(bolt);
	}

	/** hunter=false: фаза 1, призрачное появление. hunter=true: фаза 2, охотится. */
	public static void appear(ServerPlayerEntity p, boolean hunter) {
		ServerWorld w = p.getServerWorld();
		if (hunter && !w.getEntitiesByClass(MimicEntity.class, p.getBoundingBox().expand(100), e -> true).isEmpty()) return;
		for (int i = 0; i < 15; i++) {
			Vec3d o = offset(p, 10, 30);
			BlockPos g = findGround(w, (int) Math.floor(o.x), (int) Math.floor(o.z), p.getBlockPos().getY());
			if (g == null) continue;
			MimicEntity m = MimicMod.MIMIC.create(w);
			if (m == null) return;
			m.refreshPositionAndAngles(g.getX() + 0.5, g.getY(), g.getZ() + 0.5, 0.0F, 0.0F);
			if (hunter) {
				m.setHunter(2400);
				m.setTarget(p);
			} else {
				m.setHaunt(true);
			}
			w.spawnEntity(m);
			w.playSound(null, m.getX(), m.getY(), m.getZ(), hunter ? MimicSounds.SEE_YOU : MimicSounds.HI,
					SoundCategory.HOSTILE, 3.0F, 1.0F);
			return;
		}
	}

	// ------------------------------------------------------------ утилиты

	/** Случайная точка на расстоянии min..max от игрока; в 40% случаев - за спиной. */
	private static Vec3d offset(ServerPlayerEntity p, double min, double max) {
		double yawRad = Math.toRadians(p.yaw);
		double ang = RNG.nextInt(100) < 40
				? yawRad + Math.PI + (RNG.nextDouble() - 0.5) * 1.6
				: RNG.nextDouble() * Math.PI * 2;
		double d = min + RNG.nextDouble() * (max - min);
		return new Vec3d(p.getX() - Math.sin(ang) * d, p.getY(), p.getZ() + Math.cos(ang) * d);
	}

	private static BlockPos findGround(ServerWorld w, int x, int z, int aroundY) {
		if (!w.isChunkLoaded(new BlockPos(x, 0, z))) return null;
		for (int dy = 8; dy >= -12; dy--) {
			BlockPos pos = new BlockPos(x, aroundY + dy, z);
			BlockPos below = pos.down();
			if (w.getBlockState(below).isSolidBlock(w, below)
					&& w.getBlockState(pos).isAir()
					&& w.getBlockState(pos.up()).isAir()
					&& w.getBlockState(pos.up(2)).isAir()) {
				return pos;
			}
		}
		return null;
	}

	private static void playTo(ServerPlayerEntity p, SoundEvent s, Vec3d pos, float vol, float pitch) {
		p.networkHandler.sendPacket(new PlaySoundS2CPacket(s, SoundCategory.HOSTILE, pos.x, pos.y, pos.z, vol, pitch));
	}
}
