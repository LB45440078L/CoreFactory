package io.corefactory.core.mechanics;

import io.corefactory.api.TntDefinition;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * The nuclear family of TNT types, read from the definitions: every type whose mechanic is {@code nuclear} or {@code neutron}.
 * Nothing names a type here, so a new nuclear tier in tnts.yml is nuclear without a code change.
 */
public final class NuclearTnts {

    private static final Set<String> MECHANICS = Set.of(NuclearMechanic.ID, NeutronMechanic.ID);

    private NuclearTnts() {
    }

    /** True when the type's mechanic is {@code nuclear} or {@code neutron}. */
    public static boolean isNuclear(TntDefinition definition) {
        return MECHANICS.contains(definition.mechanic());
    }

    /** The ids of the nuclear types among {@code definitions}, in alphabetical order. */
    public static Set<String> idsOf(Collection<TntDefinition> definitions) {
        Set<String> ids = new TreeSet<>();
        for (TntDefinition definition : definitions) {
            if (isNuclear(definition)) {
                ids.add(definition.id());
            }
        }
        return Collections.unmodifiableSet(ids);
    }
}
