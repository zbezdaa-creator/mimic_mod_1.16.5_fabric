package com.mimicmod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

/** "Режиссёр": сам решает, когда и что происходит рядом с каждым игроком. */
public class MimicDirector {
	/** Через сколько игровых дней начинается фаза 2 (мимик преследует и убивает). */
	public static final int PHASE2_DAY = 3;
	/** 0 = авто, 1 или 2 = принудительно (команда /mimic phase). */
	public static int forcedPhase = 0;

	private static final Random RNG = new Random();
	private static final Map<UUID, Integer> timers = new HashMap<>();
	private static final Map<UUID, Amb> amb = new HashMap<>();
	private static final List<Task> tasks = new ArrayList<>();
	private static int now = 0;

	private static class Task {
		final int at;
		final Runnable run;
		Task(int at, Runnable run) { this.at = at; this.run = run; }
	}

	/** Какой длинный фоновый трек сейчас играет у игрока. */
	private static class Amb {
		SoundEvent current;
		int until;
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

	/** Мимик исчез, потому что на него посмотрели - скоро вернётся. */
	public static void hurry(UUID id) {
		timers.put(id, 200 + RNG.nextInt(400));
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
			if (r < 14) ambient(p);
			else if (r < 24) farScream(p);
			else if (r < 34) footsteps(p);
			else if (r < 42) digging(p);
			else if (r < 50) rustle(p);
			else if (r < 60) chat(p);
			else if (r < 68) screenText(p);
			else if (r < 80) appear(p, MimicEntity.FLICKER);
			else if (r < 90) appear(p, MimicEntity.GHOST);
			else weather(p);
		} else {
			if (r < 10) ambient(p);
			else if (r < 16) farScream(p);
			else if (r < 24) footsteps(p);
			else if (r < 30) digging(p);
			else if (r < 36) rustle(p);
			else if (r < 42) chat(p);
			else if (r < 48) screenText(p);
			else if (r < 58) appear(p, MimicEntity.FLICKER);
			else if (r < 80) appear(p, MimicEntity.STALK);
			else if (r < 90) weather(p);
			else appear(p, MimicEntity.GHOST);
		}
	}

	private static void ambient(ServerPlayerEntity p) {
		if (RNG.nextInt(100) < 65) playAmbient(p, MimicSounds.DRONE, MimicSounds.DRONE_TICKS);
		else playAmbient(p, MimicSounds.WATERPHONE, MimicSounds.WATERPHONE_TICKS);
	}

	/** Мимик рядом: включаем жуткую музыку (если играет гул - заглушаем его). */
	public static void nearMimic(ServerPlayerEntity p) {
		playAmbient(p, MimicSounds.WATERPHONE, MimicSounds.WATERPHONE_TICKS);
	}

	/** Два длинных трека никогда не играют одновременно. */
	private static void playAmbient(ServerPlayerEntity p, SoundEvent track, int ticks) {
		Amb a = amb.get(p.getUuid());
		if (a == null) { a = new Amb(); amb.put(p.getUuid(), a); }
		if (a.current != null && now < a.until) {
			if (a.current == track) return;
			p.networkHandler.sendPacket(new StopSoundS2CPacket(a.current.getId(), SoundCategory.AMBIENT));
		}
		p.networkHandler.sendPacket(new PlaySoundS2CPacket(track, SoundCategory.AMBIENT, p.getX(), p.getY(), p.getZ(), 1.0F, 1.0F));
		a.current = track;
		a.until = now + ticks;
	}

	/** Далёкий крик с эхом. */
	private static void farScream(ServerPlayerEntity p) {
		playTo(p, MimicSounds.FAR_SCREAM, offset(p, 30, 45), 3.0F, 1.0F);
	}

	private static void rustle(ServerPlayerEntity p) {
		playTo(p, MimicSounds.RUSTLE, offset(p, 6, 20), 1.6F, 0.9F + RNG.nextFloat() * 0.2F);
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

	private static void screenText(ServerPlayerEntity p) {
		String[] lines = {"I SEE YOU", "BEHIND YOU", "DON'T LOOK BACK", "HI", "I'M HERE", "RUN"};
		String s = lines[RNG.nextInt(lines.length)];
		if (RNG.nextInt(100) < 30) s = new StringBuilder(s).reverse().toString();
		sendText(p, s, 50 + RNG.nextInt(30));
	}

	private static void weather(ServerPlayerEntity p) {
		ServerWorld w = p.getServerWorld();
		if (w.getRegistryKey() != net.minecraft.world.World.OVERWORLD) { rustle(p); return; }
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

	/** Появление мимика. mode: MimicEntity.GHOST / FLICKER / STALK / HUNT. */
	public static void appear(ServerPlayerEntity p, int mode) {
		ServerWorld w = p.getServerWorld();
		if (!w.getEntitiesByClass(MimicEntity.class, p.getBoundingBox().expand(100), e -> true).isEmpty()) return;
		for (int i = 0; i < 15; i++) {
			Vec3d o;
			if (mode == MimicEntity.FLICKER && RNG.nextBoolean()) o = offsetFront(p, 5, 22); // прямо перед игроком
			else if (mode == MimicEntity.STALK || mode == MimicEntity.HUNT) o = offset(p, 18, 32);
			else o = offset(p, 10, 30);
			BlockPos g = findGround(w, (int) Math.floor(o.x), (int) Math.floor(o.z), p.getBlockPos().getY());
			if (g == null) continue;
			MimicEntity m = MimicMod.MIMIC.create(w);
			if (m == null) return;
			m.refreshPositionAndAngles(g.getX() + 0.5, g.getY(), g.getZ() + 0.5, 0.0F, 0.0F);
			m.setMode(mode == MimicEntity.HUNT ? MimicEntity.STALK : mode);
			w.spawnEntity(m);
			if (mode == MimicEntity.FLICKER) {
				w.playSound(null, m.getX(), m.getY(), m.getZ(), MimicSounds.FLICKER, SoundCategory.HOSTILE, 3.0F, 1.0F);
			}
			if (mode == MimicEntity.HUNT) m.beginHunt(p);
			return;
		}
	}

	// ------------------------------------------------------------ сеть (надписи и скример)

	public static void sendText(ServerPlayerEntity p, String text, int ticks) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(text);
		buf.writeInt(ticks);
		ServerPlayNetworking.send(p, MimicNet.TEXT, buf);
	}

	public static void jumpscare(ServerPlayerEntity p) {
		ServerPlayNetworking.send(p, MimicNet.SCARE, PacketByteBufs.create());
	}

	// ------------------------------------------------------------ утилиты

	private static Vec3d offsetAngle(ServerPlayerEntity p, double ang, double min, double max) {
		double d = min + RNG.nextDouble() * (max - min);
		return new Vec3d(p.getX() - Math.sin(ang) * d, p.getY(), p.getZ() + Math.cos(ang) * d);
	}

	/** Случайная точка на расстоянии min..max; в 40% случаев - за спиной. */
	private static Vec3d offset(ServerPlayerEntity p, double min, double max) {
		double yawRad = Math.toRadians(p.yaw);
		double ang = RNG.nextInt(100) < 40
				? yawRad + Math.PI + (RNG.nextDouble() - 0.5) * 1.6
				: RNG.nextDouble() * Math.PI * 2;
		return offsetAngle(p, ang, min, max);
	}

	private static Vec3d offsetFront(ServerPlayerEntity p, double min, double max) {
		double ang = Math.toRadians(p.yaw) + (RNG.nextDouble() - 0.5) * 0.9;
		return offsetAngle(p, ang, min, max);
	}

	private static BlockPos findGround(ServerWorld w, int x, int z, int aroundY) {
		if (!w.isChunkLoaded(new BlockPos(x, 0, z))) return null;
		for (int dy = 8; dy >= -12; dy--) {
			BlockPos pos = new BlockPos(x, aroundY + dy, z);
			BlockPos below = pos.down();
			if (w.getBlockState(below).isSolidBlock(w, below)
					&& w.getBlockState(pos).isAir()
					&& w.getBlockState(pos.up()).isAir()
					&& w.getBlockState(pos.up(2)).isAir()
					&& w.getBlockState(pos.up(3)).isAir()) {
				return pos;
			}
		}
		return null;
	}

	private static void playTo(ServerPlayerEntity p, SoundEvent s, Vec3d pos, float vol, float pitch) {
		p.networkHandler.sendPacket(new PlaySoundS2CPacket(s, SoundCategory.HOSTILE, pos.x, pos.y, pos.z, vol, pitch));
	}
}
