package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import ac.grim.grimac.utils.data.packetentity.PacketEntityRideable;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMetadataData;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMountData;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMoveData;
import ac.grim.grimac.utils.data.packetentity.latency.EntityPropertiesData;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.entity.WrappedPacketOutEntity;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitydestroy.WrappedPacketOutEntityDestroy;
import io.github.retrooper.packetevents.packetwrappers.play.out.entityeffect.WrappedPacketOutEntityEffect;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedPacketOutEntityMetadata;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitystatus.WrappedPacketOutEntityStatus;
import io.github.retrooper.packetevents.packetwrappers.play.out.entityteleport.WrappedPacketOutEntityTeleport;
import io.github.retrooper.packetevents.packetwrappers.play.out.mount.WrappedPacketOutMount;
import io.github.retrooper.packetevents.packetwrappers.play.out.namedentityspawn.WrappedPacketOutNamedEntitySpawn;
import io.github.retrooper.packetevents.packetwrappers.play.out.removeentityeffect.WrappedPacketOutRemoveEntityEffect;
import io.github.retrooper.packetevents.packetwrappers.play.out.spawnentity.WrappedPacketOutSpawnEntity;
import io.github.retrooper.packetevents.packetwrappers.play.out.spawnentityliving.WrappedPacketOutSpawnEntityLiving;
import io.github.retrooper.packetevents.packetwrappers.play.out.updateattributes.WrappedPacketOutUpdateAttributes;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import io.github.retrooper.packetevents.utils.versionlookup.viaversion.ViaVersionLookupUtils;
import it.unimi.dsi.fastutil.Pair;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffectType;

public class PacketEntityReplication extends PacketListenerAbstract {

    public PacketEntityReplication() {
        super(PacketListenerPriority.MONITOR);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.SPAWN_ENTITY_SPAWN || packetID == PacketType.Play.Server.SPAWN_ENTITY_LIVING) {
            WrappedPacketOutSpawnEntityLiving packetOutEntity = new WrappedPacketOutSpawnEntityLiving(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            Entity entity = packetOutEntity.getEntity();
            if (entity == null) return;

            player.compensatedEntities.addEntity(packetOutEntity.getEntityId(), entity.getType(), packetOutEntity.getPosition());
        }

        if (packetID == PacketType.Play.Server.SPAWN_ENTITY) {
            WrappedPacketOutSpawnEntity packetOutEntity = new WrappedPacketOutSpawnEntity(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            Entity entity = packetOutEntity.getEntity();
            if (entity == null) return;

            player.compensatedEntities.addEntity(packetOutEntity.getEntityId(), entity.getType(), packetOutEntity.getPosition());
        }

        if (packetID == PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
            WrappedPacketOutNamedEntitySpawn spawn = new WrappedPacketOutNamedEntitySpawn(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            Entity entity = spawn.getEntity();
            if (entity == null) return;

            player.compensatedEntities.addEntity(spawn.getEntityId(), entity.getType(), spawn.getPosition());

            player.reach.handleSpawnPlayer(spawn.getEntityId(), spawn.getPosition());
        }

        if (packetID == PacketType.Play.Server.REL_ENTITY_MOVE || packetID == PacketType.Play.Server.REL_ENTITY_MOVE_LOOK) {
            WrappedPacketOutEntity.WrappedPacketOutRelEntityMove move = new WrappedPacketOutEntity.WrappedPacketOutRelEntityMove(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            if (move.getDeltaX() != 0 || move.getDeltaY() != 0 || move.getDeltaZ() != 0)
                player.compensatedEntities.moveEntityQueue.add(new EntityMoveData(move.getEntityId(),
                        move.getDeltaX(), move.getDeltaY(), move.getDeltaZ(), player.lastTransactionSent.get(), true));


            if (player.reach.entityMap.containsKey(move.getEntityId())) {
                player.sendAndFlushTransactionOrPingPong();
                player.reach.handleMoveEntity(move.getEntityId(), move.getDeltaX(), move.getDeltaY(), move.getDeltaZ(), true);
                event.setPostTask(() -> player.sendTransactionOrPingPong(player.getNextTransactionID(1), true));
            }
        }

        if (packetID == PacketType.Play.Server.ENTITY_TELEPORT) {
            WrappedPacketOutEntityTeleport teleport = new WrappedPacketOutEntityTeleport(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            Vector3d position = teleport.getPosition();

            player.compensatedEntities.moveEntityQueue.add(new EntityMoveData(teleport.getEntityId(),
                    position.getX(), position.getY(), position.getZ(), player.lastTransactionSent.get(), false));

            if (player.reach.entityMap.containsKey(teleport.getEntityId())) {
                player.sendAndFlushTransactionOrPingPong();
                player.reach.handleMoveEntity(teleport.getEntityId(), teleport.getPosition().getX(),
                        teleport.getPosition().getY(), teleport.getPosition().getZ(), false);
                event.setPostTask(() -> player.sendTransactionOrPingPong(player.getNextTransactionID(1), true));
            }
        }

        if (packetID == PacketType.Play.Server.ENTITY_METADATA) {
            WrappedPacketOutEntityMetadata entityMetadata = new WrappedPacketOutEntityMetadata(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            player.compensatedEntities.importantMetadataQueue.add(new EntityMetadataData(entityMetadata.getEntityId(), entityMetadata.getWatchableObjects(), player.lastTransactionSent.get()));
        }

        if (packetID == PacketType.Play.Server.ENTITY_EFFECT) {
            WrappedPacketOutEntityEffect effect = new WrappedPacketOutEntityEffect(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            PotionEffectType type = PotionEffectType.getById(effect.getEffectId());

            // ViaVersion tries faking levitation effects and fails badly lol, flagging the anticheat
            // Block other effects just in case ViaVersion gets any ideas
            //
            // Set to 24 so ViaVersion blocks it
            // 24 is the levitation effect
            if (player.getClientVersion().isOlderThan(ClientVersion.v_1_9) && ViaVersionLookupUtils.isAvailable() && effect.getEffectId() > 23) {
                effect.setEffectId(24); // Just in case cancelling doesn't work
                event.setCancelled(true);
                return;
            }

            // ViaVersion dolphin's grace also messes us up, set it to a potion effect that doesn't exist on 1.12
            // Effect 31 is bad omen
            if (player.getClientVersion().isOlderThan(ClientVersion.v_1_13) && ViaVersionLookupUtils.isAvailable() && effect.getEffectId() == 30) {
                effect.setEffectId(31); // Just in case cancelling doesn't work
                event.setCancelled(true);
                return;
            }

            player.compensatedPotions.addPotionEffect(type.getName(), effect.getAmplifier(), effect.getEntityId());
        }

        if (packetID == PacketType.Play.Server.REMOVE_ENTITY_EFFECT) {
            WrappedPacketOutRemoveEntityEffect effect = new WrappedPacketOutRemoveEntityEffect(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            player.compensatedPotions.removePotionEffect(PotionEffectType.getById(effect.getEffectId()).getName(), effect.getEntityId());
        }

        if (packetID == PacketType.Play.Server.UPDATE_ATTRIBUTES) {
            WrappedPacketOutUpdateAttributes attributes = new WrappedPacketOutUpdateAttributes(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            int entityID = attributes.getEntityId();

            PacketEntity entity = player.compensatedEntities.getEntity(attributes.getEntityId());
            if (player.entityID == entityID || entity instanceof PacketEntityHorse || entity instanceof PacketEntityRideable) {
                event.setPostTask(player::sendAndFlushTransactionOrPingPong);
                player.compensatedEntities.entityPropertiesData.add(new EntityPropertiesData(entityID, attributes.getProperties(), player.lastTransactionSent.get() + 1));
            }
        }

        if (packetID == PacketType.Play.Server.ENTITY_STATUS) {
            WrappedPacketOutEntityStatus status = new WrappedPacketOutEntityStatus(event.getNMSPacket());
            // This hasn't changed from 1.7.2 to 1.17
            // Needed to exempt players on dead vehicles, as dead entities have strange physics.
            if (status.getEntityStatus() == 3) {
                GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
                if (player == null) return;

                PacketEntity entity = player.compensatedEntities.getEntity(status.getEntityId());

                if (entity == null) return;
                entity.isDead = true;
            }
        }

        if (packetID == PacketType.Play.Server.MOUNT) {
            WrappedPacketOutMount mount = new WrappedPacketOutMount(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            int vehicleID = mount.getEntityId();

            if (player.packetStateData.vehicle != null && player.packetStateData.vehicle == vehicleID)
                player.packetStateData.vehicle = null;

            int[] passengers = mount.getPassengerIds();

            if (passengers != null) {
                for (int entityID : passengers) {
                    // Handle scenario transferring from entity to entity with the following packet order:
                    // Player boards the new entity and a packet is sent for that
                    // Player is removed from the old entity
                    // Without the second check the player wouldn't be riding anything
                    if (player.entityID == entityID) {
                        player.packetStateData.vehicle = vehicleID;
                        break;
                    }
                }
            }

            player.compensatedEntities.mountVehicleQueue.add(new EntityMountData(vehicleID, passengers, player.lastTransactionSent.get()));
        }

        if (packetID == PacketType.Play.Server.ENTITY_DESTROY) {
            WrappedPacketOutEntityDestroy destroy = new WrappedPacketOutEntityDestroy(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            int lastTransactionSent = player.lastTransactionSent.get();
            int[] destroyEntityIds = destroy.getEntityIds();

            player.compensatedEntities.destroyEntityQueue.add(new Pair<Integer, int[]>() {
                @Override
                public Integer left() {
                    return lastTransactionSent;
                }

                @Override
                public int[] right() {
                    return destroyEntityIds;
                }
            });
        }
    }
}
