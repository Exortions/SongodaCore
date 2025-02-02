package com.songoda.core.nms.v1_11_R1.world;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.compatibility.CompatibleParticleHandler;
import com.songoda.core.nms.world.SSpawner;
import com.songoda.core.nms.world.SpawnedEntity;
import net.minecraft.server.v1_11_R1.BlockPosition;
import net.minecraft.server.v1_11_R1.ChunkRegionLoader;
import net.minecraft.server.v1_11_R1.DifficultyDamageScaler;
import net.minecraft.server.v1_11_R1.Entity;
import net.minecraft.server.v1_11_R1.EntityInsentient;
import net.minecraft.server.v1_11_R1.MobSpawnerData;
import net.minecraft.server.v1_11_R1.NBTTagCompound;
import net.minecraft.server.v1_11_R1.WorldServer;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_11_R1.CraftWorld;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Random;
import java.util.Set;

public class SSpawnerImpl implements SSpawner {
    private final Location spawnerLocation;

    public SSpawnerImpl(Location location) {
        this.spawnerLocation = location;
    }

    @Override
    public LivingEntity spawnEntity(EntityType type, Location spawnerLocation) {
        return spawnEntity(type, "EXPLOSION_NORMAL", null, null);
    }

    @Override
    public LivingEntity spawnEntity(EntityType type, String particleType, SpawnedEntity spawned,
                                    Set<CompatibleMaterial> canSpawnOn) {
        MobSpawnerData data = new MobSpawnerData();
        NBTTagCompound compound = data.b();

        String name = type.name().toLowerCase().replace("snowman", "snow_golem")
                .replace("mushroom_cow", "mooshroom");
        compound.setString("id", "minecraft:" + name);

        short spawnRange = 4;
        for (int i = 0; i < 50; i++) {
            WorldServer world = ((CraftWorld) spawnerLocation.getWorld()).getHandle();

            Random random = world.random;
            double x = spawnerLocation.getX() + (random.nextDouble() - random.nextDouble()) * (double) spawnRange + 0.5D;
            double y = spawnerLocation.getY() + random.nextInt(3) - 1;
            double z = spawnerLocation.getZ() + (random.nextDouble() - random.nextDouble()) * (double) spawnRange + 0.5D;

            Entity entity = ChunkRegionLoader.a(compound, world, x, y, z, false);

            assert entity != null;
            BlockPosition position = entity.getChunkCoordinates();
            DifficultyDamageScaler damageScaler = world.D(position);

            if (!(entity instanceof EntityInsentient)) {
                continue;
            }

            EntityInsentient entityInsentient = (EntityInsentient) entity;

            Location spot = new Location(spawnerLocation.getWorld(), x, y, z);
            if (!canSpawn(entityInsentient, spot, canSpawnOn)) {
                continue;
            }

            entityInsentient.prepare(damageScaler, null);

            LivingEntity craftEntity = (LivingEntity) entity.getBukkitEntity();

            if (spawned != null && !spawned.onSpawn(craftEntity)) {
                return null;
            }

            if (particleType != null) {
                float xx = (float) (0 + (Math.random() * 1));
                float yy = (float) (0 + (Math.random() * 2));
                float zz = (float) (0 + (Math.random() * 1));

                CompatibleParticleHandler.spawnParticles(CompatibleParticleHandler.ParticleType.getParticle(particleType),
                        spot, 5, xx, yy, zz, 0);
            }

            world.addEntity(entity, CreatureSpawnEvent.SpawnReason.SPAWNER);

            spot.setYaw(random.nextFloat() * 360.0F);
            craftEntity.teleport(spot);

            return craftEntity;
        }

        return null;
    }

    private boolean canSpawn(EntityInsentient entityInsentient, Location location, Set<CompatibleMaterial> canSpawnOn) {
        if (!entityInsentient.canSpawn()) {
            return false;
        }

        CompatibleMaterial spawnedIn = CompatibleMaterial.getMaterial(location.getBlock());
        CompatibleMaterial spawnedOn = CompatibleMaterial.getMaterial(location.getBlock().getRelative(BlockFace.DOWN));

        if (spawnedIn == null || spawnedOn == null) {
            return false;
        }

        if (!spawnedIn.isAir() &&
                spawnedIn != CompatibleMaterial.WATER &&
                !spawnedIn.name().contains("PRESSURE") &&
                !spawnedIn.name().contains("SLAB")) {
            return false;
        }

        for (CompatibleMaterial material : canSpawnOn) {
            if (material == null) continue;

            if (spawnedOn.equals(material) || material.isAir()) {
                return true;
            }
        }

        return false;
    }
}
