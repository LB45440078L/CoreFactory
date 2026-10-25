package io.corefactory.paper.gui;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

/**
 * The sounds of the catalogue and the recipe views. Each one plays to the clicking player only, at their own position, so other
 * players do not hear the menu. A menu calls one of these only when its click does something: a blank pane, a locked entry or an
 * entry with no recipe stays silent.
 */
final class GuiSounds {

    private static final float VOLUME = 0.5f;

    private GuiSounds() {
    }

    /** A plain click: a tab, an entry that opens its recipe, or an ingredient that opens its own recipe. */
    static void click(Player player) {
        play(player, Sound.UI_BUTTON_CLICK, 1.0f);
    }

    /** A page turn: the next and previous page arrows, and the arrows that scroll the category tabs. */
    static void page(Player player) {
        play(player, Sound.ITEM_BOOK_PAGE_TURN, 1.0f);
    }

    /** A back button, lower than a click: back to the view that opened this one, or to the catalogue, and Close. */
    static void back(Player player) {
        play(player, Sound.UI_BUTTON_CLICK, 0.8f);
    }

    /** An item handed out to the player from the catalogue. */
    static void take(Player player) {
        play(player, Sound.ENTITY_ITEM_PICKUP, 1.0f);
    }

    private static void play(Player player, Sound sound, float pitch) {
        player.playSound(player.getLocation(), sound, SoundCategory.UI, VOLUME, pitch);
    }
}
