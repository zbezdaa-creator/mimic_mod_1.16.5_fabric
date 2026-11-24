package com.mimicmod;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MimicEntity extends HostileEntity {
	/** Стоит и смотрит, исчезает если на него посмотреть. */
	public static final int GHOST = 0;
	/** Мелькнул на секунду и пропал. */
	public static final int FLICKER = 1;
	/** Медленно преследует, когда на него не смотрят; потом нападает. */
	public static final int STALK = 2;
	/** Бежит на игрока и убивает. */
	public static final int HUNT = 3;

	private int mode = STALK;
	private int life = 0;
	private int lookTicks = 0;
	private int stalkTime = 600;
	private int flickerLife = 20;
	private int huntTicks = 0;
	private int scareCooldown = 0;

	public MimicEntity(EntityType<? extends MimicEntity> type, World world) {
		super(type, world);
		this.setInvulnerable(true);
	}

	public static DefaultAttributeContainer.Builder createMimicAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0D)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.36D)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 12.0D)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 80.0D);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(1, new MeleeAttackGoal(this, 1.3D, false));
		this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 40.0F));
		// Цель (игрока) задаём вручную, когда начинается охота
	}

	public void setMode(int m) {
		this.mode = m;
		boolean still = (m == GHOST || m == FLICKER);
		this.setAiDisabled(still);
		this.setPersistent();
		if (m == STALK) stalkTime = 400 + this.random.nextInt(800); // 20-60 сек до нападения
		if (m == FLICKER) flickerLife = 12 + this.random.nextInt(20);
	}

	public void beginHunt(PlayerEntity p) {
		this.mode = HUNT;
		this.huntTicks = 0;
		this.setAiDisabled(false);
		this.setTarget(p);
		this.world.playSound(null, this.getX(), this.getY(), this.getZ(), MimicSounds.SEE_YOU,
				SoundCategory.HOSTILE, 3.0F, 1.0F);
		if (p instanceof ServerPlayerEntity) {
			MimicDirector.sendText((ServerPlayerEntity) p, "I SEE YOU", 80);
		}
	}

	private boolean seenBy(PlayerEntity p) {
		Vec3d look = p.getRotationVec(1.0F).normalize();
		Vec3d to = new Vec3d(this.getX() - p.getX(), this.getEyeY() - p.getEyeY(), this.getZ() - p.getZ()).normalize();
		return look.dotProduct(to) > 0.94D && p.canSee(this);
	}

	private void faceInstantly(PlayerEntity p) {
		double dx = p.getX() - this.getX();
		double dz = p.getZ() - this.getZ();
		float yawToPlayer = (float) (MathHelper.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
		this.yaw = yawToPlayer;
		this.bodyYaw = yawToPlayer;
		this.headYaw = yawToPlayer;
		this.pitch = 0.0F;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.world.isClient) return;
		life++;
		if (scareCooldown > 0) scareCooldown--;
		if (life > 6000) { vanish(false); return; }

		PlayerEntity p = this.world.getClosestPlayer(this, 90.0D);
		if (p == null) { if (life > 100) vanish(false); return; }

		if ((mode == STALK || mode == HUNT) && this.isAiDisabled()) this.setAiDisabled(false);

		// "жуткая музыка", когда он рядом
		if (life % 20 == 0 && p instanceof ServerPlayerEntity && this.distanceTo(p) < 40.0F) {
			MimicDirector.nearMimic((ServerPlayerEntity) p);
		}

		if (mode == GHOST) {
			faceInstantly(p);
			if (this.distanceTo(p) < 4.0F) { vanish(true); return; }
			if (seenBy(p)) {
				lookTicks++;
				if (lookTicks >= 20) { vanish(true); return; }
			}
			if (life > 300) vanish(false);

		} else if (mode == FLICKER) {
			faceInstantly(p);
			if (life >= flickerLife) vanish(false);

		} else if (mode == STALK) {
			this.getLookControl().lookAt(p, 60.0F, 60.0F);
			if (seenBy(p)) {
				lookTicks++;
				this.getNavigation().stop();
				if (lookTicks >= 40) { // посмотрели ~2 сек - исчезает и вернётся позже
					if (p instanceof ServerPlayerEntity) MimicDirector.hurry(p.getUuid());
					vanish(true);
					return;
				}
			} else {
				if (lookTicks > 0) lookTicks--;
				if (life % 10 == 0 && this.distanceTo(p) > 5.0F) {
					this.getNavigation().startMovingTo(p, 0.9D);
				}
			}
			if (life > stalkTime) beginHunt(p);

		} else if (mode == HUNT) {
			huntTicks++;
			if (this.getTarget() == null) this.setTarget(p);
			if (huntTicks > 1200) vanish(true);
		}
	}

	private void vanish(boolean sound) {
		if (sound) {
			this.world.playSound(null, this.getX(), this.getY(), this.getZ(), MimicSounds.VANISH,
					SoundCategory.HOSTILE, 3.0F, 1.0F);
		}
		this.remove();
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (source == DamageSource.OUT_OF_WORLD) return super.damage(source, amount); // /kill работает
		if (!this.world.isClient && source.getAttacker() instanceof PlayerEntity && mode != HUNT) {
			vanish(true); // ударили - исчезает
		}
		return false; // во время охоты неуязвим
	}

	@Override
	public boolean tryAttack(Entity target) {
		boolean hit = super.tryAttack(target);
		if (hit && target instanceof ServerPlayerEntity && scareCooldown == 0) {
			MimicDirector.jumpscare((ServerPlayerEntity) target);
			scareCooldown = 600;
		}
		return hit;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(MimicSounds.FOOTSTEP, 0.8F, 0.8F);
	}
}
