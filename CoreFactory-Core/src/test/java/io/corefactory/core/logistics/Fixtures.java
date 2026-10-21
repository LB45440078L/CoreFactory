package io.corefactory.core.logistics;

import io.corefactory.core.energy.Pos;

import java.util.LinkedHashMap;
import java.util.Map;

/** Shared cells and items for the logistics tests. Everything sits on y 64. */
final class Fixtures {

    static final int Y = 64;

    static final Item A = Item.material("A");
    static final Item B = Item.material("B");
    static final Item C = Item.material("C");

    private Fixtures() {
    }

    static Pos at(int x, int z) {
        return new Pos(x, Y, z);
    }

    static String belt(int x, int z) {
        return LogisticsNetwork.beltId(at(x, z));
    }

    static Item[] many(Item item, int count) {
        Item[] items = new Item[count];
        java.util.Arrays.fill(items, item);
        return items;
    }

    static Map<Pos, Port> ports(Object... pairs) {
        Map<Pos, Port> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((Pos) pairs[i], (Port) pairs[i + 1]);
        }
        return map;
    }

    static void tickTimes(LogisticsNetwork network, int ticks, double dt, Map<Pos, Port> ports) {
        for (int i = 0; i < ticks; i++) {
            network.tick(dt, Map.of(), ports);
        }
    }
}
