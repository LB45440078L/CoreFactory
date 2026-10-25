package io.corefactory.paper.gui.catalog;

import java.util.List;
import java.util.Optional;

/**
 * A group of the catalogue, shown as one tab.
 *
 * @param title   the tab's name, as plain text
 * @param iconId  the id of the entry whose item is the tab's icon; it should be one of the entries
 * @param entries the items of the category, in the order they are shown
 */
public record CatalogCategory(String title, String iconId, List<CatalogEntry> entries) {

    public CatalogCategory {
        entries = List.copyOf(entries);
    }

    /** The entry the tab's icon is drawn from, or empty when the icon id is not one of the entries. */
    public Optional<CatalogEntry> icon() {
        return entries.stream().filter(entry -> entry.id().equals(iconId)).findFirst();
    }
}
