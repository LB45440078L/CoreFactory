package io.corefactory.spigot.command;

import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.recipe.RecipeSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Tab completion as pure functions: which suggestions match the typed text, how they are ordered, which ids a subcommand
 * takes, and which suggestions the sender may use. Nothing here touches the server, so the rules are tested with the
 * shipped ids.
 *
 * <p>A suggestion is plain text, because the server cannot colour it. The chat messages around it use {@link Style}.
 */
public final class Completion {

    private Completion() {
    }

    /**
     * A suggestion and the permission that its subcommand needs.
     *
     * @param text       the suggestion
     * @param permission the permission a sender needs to be offered it, or {@code null} when anyone is
     */
    public record Candidate(String text, String permission) {
    }

    /**
     * The suggestions that start with the typed text, ignoring case, that the sender may use, in alphabetical order.
     *
     * @param candidates     every suggestion for this position
     * @param typed          the text typed so far; empty offers all of them
     * @param hasPermission  whether the sender has a permission node
     * @return the suggestions, alphabetical, without repeats
     */
    public static List<String> complete(Collection<Candidate> candidates, String typed, Predicate<String> hasPermission) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(candidate -> candidate.permission() == null || hasPermission.test(candidate.permission()))
                .map(Candidate::text)
                .filter(text -> text.toLowerCase(Locale.ROOT).startsWith(prefix))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    /** The candidates for a list of texts that all need the same permission ({@code null} for none). */
    public static List<Candidate> all(Collection<String> texts, String permission) {
        List<Candidate> candidates = new ArrayList<>();
        for (String text : texts) {
            candidates.add(new Candidate(text, permission));
        }
        return candidates;
    }

    /** The ids {@code /cf give} takes: every TNT type, component and ore block. */
    public static List<String> giveIds(Collection<String> tntIds, Collection<String> componentIds,
                                       Collection<String> oreIds) {
        List<String> ids = new ArrayList<>(tntIds);
        ids.addAll(componentIds);
        ids.addAll(oreIds);
        return ids;
    }

    /**
     * The ids {@code /cf recipe} can show: the TNT types and components that declare a recipe. A TNT with a recipe
     * entry but no definition is left out, as the recipe registrar leaves it out. Ore blocks have no recipe.
     *
     * @param tntIds      the defined TNT ids
     * @param tntRecipes  the recipes of the TNT types, keyed by TNT id
     * @param components  the component definitions, keyed by id
     */
    public static List<String> recipeIds(Set<String> tntIds, Map<String, RecipeSpec> tntRecipes,
                                         Map<String, ComponentDefinition> components) {
        List<String> ids = new ArrayList<>();
        for (String id : tntRecipes.keySet()) {
            if (tntIds.contains(id)) {
                ids.add(id);
            }
        }
        for (ComponentDefinition component : components.values()) {
            if (component.recipe() != null) {
                ids.add(component.id());
            }
        }
        return ids;
    }
}
