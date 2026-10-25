package io.corefactory.paper.turret;

import io.corefactory.core.turret.TurretType;

import java.util.Arrays;
import java.util.Optional;

/**
 * The kinds of turret. A machine gun turret fires rounds from a magazine. A laser turret is electric: it holds no rounds and fires an
 * instant beam. A rail gun is electric too: it charges for a long wind-up and then fires one heavy shot, which the rail gun's own runtime
 * sends. Each kind has the item that places it, the device type it is saved under, and its levels in the configuration.
 */
public enum TurretKind {

    MACHINE_GUN(TurretIds.ITEM, TurretIds.DEVICE_TYPE, TurretType.MACHINE_GUN),
    LASER(TurretIds.LASER_ITEM, TurretIds.LASER_DEVICE_TYPE, TurretType.LASER),
    RAILGUN(TurretIds.RAILGUN_ITEM, TurretIds.RAILGUN_DEVICE_TYPE, TurretType.RAILGUN);

    private final String itemId;
    private final String deviceType;
    private final TurretType type;

    TurretKind(String itemId, String deviceType, TurretType type) {
        this.itemId = itemId;
        this.deviceType = deviceType;
        this.type = type;
    }

    /** The component id of the item that places this kind. */
    public String itemId() {
        return itemId;
    }

    /** The device type this kind is saved under in the turret store. */
    public String deviceType() {
        return deviceType;
    }

    /** The core kind this turret is, which names its level table and its model keys. */
    public TurretType type() {
        return type;
    }

    /** True for a kind that is electric: it holds no rounds, so its turrets draw power and need no ammunition. A laser and a rail gun are. */
    public boolean isElectric() {
        return !type.hasMagazine();
    }

    /** The kind of an item id, or empty when no turret item has it. */
    public static Optional<TurretKind> ofItem(String itemId) {
        return Arrays.stream(values()).filter(kind -> kind.itemId.equals(itemId)).findFirst();
    }

    /** The kind of a device type, or empty when no turret is saved under it. */
    public static Optional<TurretKind> ofDeviceType(String deviceType) {
        return Arrays.stream(values()).filter(kind -> kind.deviceType.equals(deviceType)).findFirst();
    }
}
