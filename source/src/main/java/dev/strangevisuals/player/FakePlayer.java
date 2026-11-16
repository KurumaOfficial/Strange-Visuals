package dev.strangevisuals.player;

import com.mojang.authlib.GameProfile;
import dev.strangevisuals.clickgui.ModuleRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public final class FakePlayer {
    private static final UUID ID = UUID.fromString("5d1b0f9c-6c4b-4a71-9f5e-2c1f7a3ba845");

    private static final double KNOCKBACK = 0.42;

    private static OtherClientPlayerEntity dummy;
    private static float health;
    private static int deadFor;
    private static long hitAt = Long.MIN_VALUE / 2;
    private static long arrivedAt = Long.MIN_VALUE / 2;
    private static int nextId = -4000;
    private static Vec3d lastImpact;

    private static final float ARRIVAL = 0.85f;

    private FakePlayer() {
    }

    public static Entity entity() {
        return dummy;
    }

    public static boolean aimedAt(MinecraftClient client) {
        if (dummy == null || client.player == null) {
            return false;
        }

        double reach = Math.max(3.2, client.player.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE));
        Vec3d eye = client.player.getEyePos();
        Vec3d end = eye.add(client.player.getRotationVec(1.0f).multiply(reach));
        return dummy.getBoundingBox().expand(0.3).raycast(eye, end).isPresent();
    }

    public static float arriving() {
        if (dummy == null) {
            return -1.0f;
        }
        float age = (System.nanoTime() - arrivedAt) / 1_000_000_000.0f / ARRIVAL;
        return age >= 1.0f ? -1.0f : age;
    }

    public static boolean is(Entity entity) {
        return dummy != null && entity == dummy;
    }

    public static float health() {
        return health;
    }

    public static float maxHealth() {
        return ModuleRegistry.fakeHealth().value();
    }

    public static void tick(MinecraftClient client) {
        boolean wanted = ModuleRegistry.fakePlayer().enabled()
            && client.world != null && client.player != null;
        if (!wanted) {
            remove(client);
            return;
        }
        if (dummy != null && dummy.getEntityWorld() != client.world) {
            dummy = null;
        }
        if (dummy == null) {
            if (deadFor > 0 && --deadFor > 0) {
                return;
            }
            spawn(client);
            return;
        }

        dummy.setVelocity(Vec3d.ZERO);
        dummy.lastX = dummy.getX();
        dummy.lastY = dummy.getY();
        dummy.lastZ = dummy.getZ();
    }

    private static void spawn(MinecraftClient client) {
        String name = client.getSession() != null ? client.getSession().getUsername() : "Dummy";
        dummy = new OtherClientPlayerEntity(client.world, new GameProfile(ID, name));

        dummy.setId(nextId--);

        Vec3d at = client.player.getEntityPos();
        Vec3d ahead = client.player.getRotationVector().multiply(2.4);
        dummy.refreshPositionAndAngles(at.x + ahead.x, at.y, at.z + ahead.z,
            client.player.getYaw() + 180.0f, 0.0f);
        dummy.setHeadYaw(dummy.getYaw());
        dummy.setBodyYaw(dummy.getYaw());

        dummy.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(maxHealth());
        dummy.setHealth(maxHealth());
        health = maxHealth();
        arrivedAt = System.nanoTime();
        client.world.addEntity(dummy);
    }

    private static void remove(MinecraftClient client) {
        if (dummy != null) {
            dummy.discard();
            dummy = null;
        }
        deadFor = 0;
    }

    public static void clear() {
        if (dummy != null) {
            dummy.discard();
            dummy = null;
        }
        deadFor = 0;
    }

    public static boolean tryHit(MinecraftClient client) {
        if (dummy == null || client.player == null || client.world == null) {
            return false;
        }

        if (sinceHit() < 0.12f) {
            return false;
        }

        double reach = Math.max(3.2, client.player.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE));
        Vec3d eye = client.player.getEyePos();
        Vec3d end = eye.add(client.player.getRotationVec(1.0f).multiply(reach));

        Box box = dummy.getBoundingBox().expand(0.3);
        java.util.Optional<Vec3d> landed = box.raycast(eye, end);
        if (landed.isEmpty()) {
            return false;
        }
        lastImpact = landed.get();
        client.player.swingHand(Hand.MAIN_HAND);
        return hit(client, dummy);
    }

    public static boolean hit(MinecraftClient client, Entity target) {
        if (!is(target) || client.player == null) {
            return false;
        }
        hitAt = System.nanoTime();

        client.player.attack(dummy);

        dummy.hurtTime = dummy.maxHurtTime = 10;
        dummy.onDamaged(client.world.getDamageSources().playerAttack(client.player));
        client.world.playSoundClient(dummy.getX(), dummy.getY(), dummy.getZ(),
            SoundEvents.ENTITY_PLAYER_HURT, dummy.getSoundCategory(), 1.0f, 1.0f, false);

        dummy.setVelocity(Vec3d.ZERO);
        dummy.velocityDirty = false;
        TargetMarks.noteHit(dummy);
        KillEffects.watch(dummy);
        HitEffects.add(dummy, 1.0f, lastImpact);

        if ("Invincible".equals(ModuleRegistry.fakeMode().current())) {
            health = maxHealth();
            dummy.setHealth(maxHealth());
            return true;
        }

        health = Math.max(0.0f, health - damageOf(client));
        dummy.setHealth(health);
        if (health <= 0.0f) {
            kill(client);
        }
        return true;
    }

    private static float sinceHit() {
        return (System.nanoTime() - hitAt) / 1_000_000_000.0f;
    }

    private static float damageOf(MinecraftClient client) {
        float base = (float) client.player.getAttributeValue(
            net.minecraft.entity.attribute.EntityAttributes.ATTACK_DAMAGE);

        float perTick = Math.max(0.0001f, client.player.getAttackCooldownProgressPerTick());
        float charge = Math.min(1.0f, sinceHit() * 20.0f / perTick);
        return Math.max(0.5f, base * (0.2f + charge * charge * 0.8f));
    }

    private static void kill(MinecraftClient client) {
        dummy.setHealth(0.0f);
        client.world.playSoundClient(dummy.getX(), dummy.getY(), dummy.getZ(),
            SoundEvents.ENTITY_PLAYER_DEATH, dummy.getSoundCategory(), 1.0f, 1.0f, false);
        dummy.onDeath(client.world.getDamageSources().playerAttack(client.player));
        dummy.discard();
        dummy = null;

        deadFor = ModuleRegistry.fakeRespawn().value() ? 40 : -1;
        if (deadFor < 0) {
            ModuleRegistry.fakePlayer().toggleSilently(false);
            deadFor = 0;
        }
    }
}
