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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MimicEntity extends HostileEntity {
	/** Фаза 1: призрак - стоит, смотрит, исчезает. */
	private boolean haunt = false;
	/** Фаза 2: сколько тиков живёт охотник (-1 = бесконечно, например из яйца). */
	private int lifeLimit = -1;
	private int life = 0;
	private int lookTicks = 0;

	public MimicEntity(EntityType<? extends MimicEntity> type, World world) {
		super(type, world);
	}

	public static DefaultAttributeContainer.Builder createMimicAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0D)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.34D)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0D)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0D);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(1, new MeleeAttackGoal(this, 1.1D, false));
		this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.8D));
		this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 32.0F));
		this.goalSelector.add(4, new LookAroundGoal(this));
		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new FollowTargetGoal<>(this, PlayerEntity.class, true));
	}

	public void setHaunt(boolean h) {
		this.haunt = h;
		this.setAiDisabled(h);
		this.setInvulnerable(h);
		this.setPersistent();
	}

	public void setHunter(int ticks) {
		this.lifeLimit = ticks;
		this.setPersistent();
	}

	@Override
	public void tick() {
		super.tick();
		if (this.world.isClient) return;
		life++;

		if (haunt) {
			PlayerEntity p = this.world.getClosestPlayer(this, 80.0D);
			if (p == null || life > 240) { vanish(false); return; }

			// поворачиваемся к игроку
			double dx = p.getX() - this.getX();
			double dz = p.getZ() - this.getZ();
			float yawToPlayer = (float) (MathHelper.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
			this.yaw = yawToPlayer;
			this.bodyYaw = yawToPlayer;
			this.headYaw = yawToPlayer;
			this.pitch = 0.0F;

			if (p.distanceTo(this) < 5.0F) { vanish(true); return; }

			Vec3d look = p.getRotationVec(1.0F).normalize();
			Vec3d to = new Vec3d(this.getX() - p.getX(), this.getEyeY() - p.getEyeY(), this.getZ() - p.getZ()).normalize();
			if (look.dotProduct(to) > 0.96D && p.canSee(this)) {
				lookTicks++;
				if (lookTicks >= 25) vanish(true); // игрок посмотрел ~1.2 сек - исчезает с криком
			}
		} else if (lifeLimit > 0 && life > lifeLimit) {
			vanish(false);
		}
	}

	private void vanish(boolean scream) {
		if (scream) {
			this.world.playSound(null, this.getX(), this.getY(), this.getZ(), MimicSounds.SCREAM,
					SoundCategory.HOSTILE, 3.0F, 1.0F);
		}
		this.remove();
	}

	@Override
	public boolean tryAttack(Entity target) {
		boolean hit = super.tryAttack(target);
		if (hit) this.playSound(MimicSounds.SCREAM, 2.0F, 1.0F);
		return hit;
	}

	@Override
	protected SoundEvent getAmbientSound() { return haunt ? null : MimicSounds.SEE_YOU; }

	@Override
	protected SoundEvent getHurtSound(DamageSource source) { return MimicSounds.DONT_KILL; }

	@Override
	protected SoundEvent getDeathSound() { return MimicSounds.NOT_ME; }

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(MimicSounds.FOOTSTEP, 0.8F, 0.8F);
	}

	@Override
	protected float getSoundVolume() { return 2.0F; }

	@Override
	public void writeCustomDataToTag(CompoundTag tag) {
		super.writeCustomDataToTag(tag);
		tag.putBoolean("Haunt", haunt);
	}

	@Override
	public void readCustomDataFromTag(CompoundTag tag) {
		super.readCustomDataFromTag(tag);
		this.haunt = tag.getBoolean("Haunt");
	}
}
