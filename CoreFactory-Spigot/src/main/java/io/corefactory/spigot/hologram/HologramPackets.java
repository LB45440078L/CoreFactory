package io.corefactory.spigot.hologram;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The PacketEvents packets that show, update and hide one line of a hologram. Each line is an invisible armour stand with no
 * gravity and no hitbox, a marker; its custom name is visible and carries the line's text.
 */
final class HologramPackets {

    private static final byte INVISIBLE_FLAG = 0x20;
    private static final byte MARKER_FLAG = 0x10;
    private static final int FLAGS_INDEX = 0;
    private static final int CUSTOM_NAME_INDEX = 2;
    private static final int CUSTOM_NAME_VISIBLE_INDEX = 3;
    private static final int NO_GRAVITY_INDEX = 5;
    private static final int ARMOR_STAND_FLAGS_INDEX = 15;

    private HologramPackets() {
    }

    /** Spawns the armour stand of a line at the line's position. */
    static WrapperPlayServerSpawnEntity spawn(Hologram hologram, int line) {
        Location at = hologram.linePosition(line);
        return new WrapperPlayServerSpawnEntity(hologram.entityId(line), Optional.of(hologram.uuid(line)),
                EntityTypes.ARMOR_STAND, new Vector3d(at.getX(), at.getY(), at.getZ()), 0f, 0f, 0f, 0,
                Optional.empty());
    }

    /** The invisible flag, the custom name and the other metadata of a line's armour stand, with the line's text. */
    static WrapperPlayServerEntityMetadata metadata(Hologram hologram, int line) {
        List<EntityData<?>> data = new ArrayList<>();
        data.add(new EntityData<>(FLAGS_INDEX, EntityDataTypes.BYTE, INVISIBLE_FLAG));
        data.add(new EntityData<>(CUSTOM_NAME_INDEX, EntityDataTypes.OPTIONAL_COMPONENT,
                Optional.of(HologramJson.of(hologram.text(line)))));
        data.add(new EntityData<>(CUSTOM_NAME_VISIBLE_INDEX, EntityDataTypes.BOOLEAN, true));
        data.add(new EntityData<>(NO_GRAVITY_INDEX, EntityDataTypes.BOOLEAN, true));
        data.add(new EntityData<>(ARMOR_STAND_FLAGS_INDEX, EntityDataTypes.BYTE, MARKER_FLAG));
        return new WrapperPlayServerEntityMetadata(hologram.entityId(line), data);
    }

    /** Removes a line's armour stand from the client. */
    static WrapperPlayServerDestroyEntities destroy(Hologram hologram, int line) {
        return new WrapperPlayServerDestroyEntities(hologram.entityId(line));
    }
}
