package io.corefactory.core.radiation;

import io.corefactory.core.item.ItemFactory;

/**
 * The radiation services as one handle: the fallout the blasts leave, the suit that shields it, and the Geiger counter
 * that reads it. The core creates one and ticks it.
 */
public final class Radiation {

    private final Fallout fallout = new Fallout();
    private final RadiationSuit suit;
    private final GeigerCounter geiger;

    public Radiation(ItemFactory items) {
        this.suit = new RadiationSuit(items);
        this.geiger = new GeigerCounter(items);
    }

    public Fallout fallout() {
        return fallout;
    }

    public RadiationSuit suit() {
        return suit;
    }

    public GeigerCounter geiger() {
        return geiger;
    }

    /** Updates the doses of the players. Called every tick; the update itself runs once a second. */
    public void tick(long tick) {
        fallout.tick(tick, suit::exposure);
    }
}
