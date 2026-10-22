package io.corefactory.core.deposit;

/**
 * How rich a deposit is, from the amount left in it: {@link #POOR} under one million, {@link #RICH} from one million to just
 * under ten million, and {@link #EXCEPTIONAL} from ten million up. Each tier has a colour code, so a scan can show the tier as a
 * coloured word.
 */
public enum RichnessTier {

    /** Under one million left. */
    POOR("Poor", "&7"),
    /** One million to just under ten million left. */
    RICH("Rich", "&6"),
    /** Ten million or more left. */
    EXCEPTIONAL("Exceptional", "&d");

    /** The least amount left for a rich deposit. */
    public static final long RICH_FROM = 1_000_000L;

    /** The least amount left for an exceptional deposit. */
    public static final long EXCEPTIONAL_FROM = 10_000_000L;

    private final String label;
    private final String colour;

    RichnessTier(String label, String colour) {
        this.label = label;
        this.colour = colour;
    }

    /**
     * The tier of a remaining amount. A negative amount is poor.
     *
     * @param remaining the amount left in the deposit
     * @return its tier
     */
    public static RichnessTier of(long remaining) {
        if (remaining >= EXCEPTIONAL_FROM) {
            return EXCEPTIONAL;
        }
        if (remaining >= RICH_FROM) {
            return RICH;
        }
        return POOR;
    }

    /** The name of the tier, such as {@code Rich}, with no colour code. */
    public String label() {
        return label;
    }

    /** The name of the tier in its colour, such as {@code &6Rich}. */
    public String coloured() {
        return colour + label;
    }
}
