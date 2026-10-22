package io.corefactory.core.deposit;

import io.corefactory.core.drill.DrillText;

/**
 * The words a scan of the deposits shows: where a deposit is from the player, its tier and what is left. Each line carries ampersand
 * colour codes, as {@link DrillText} does, and the ore name is passed in already named.
 */
public final class DepositText {

    private DepositText() {
    }

    /**
     * Where a deposit is from a point, such as {@code 42 m NE}, or {@code here} when the point is at its centre.
     *
     * @param deposit the deposit
     * @param x       the point's x
     * @param z       the point's z
     */
    public static String place(Deposit deposit, int x, int z) {
        String point = Compass.point(deposit.centreX() - x, deposit.centreZ() - z);
        if (point.equals(Compass.HERE)) {
            return Compass.HERE;
        }
        return DrillText.distance(DepositScan.blocksTo(deposit, x, z)) + " " + point;
    }

    /**
     * The line of the action bar after a scan: {@code Nearest Galena Ore: 42 m NE, Rich, 3.2M left}, with the tier coloured.
     *
     * @param oreName the ore's name, with colour codes
     * @param deposit the nearest deposit
     * @param x       the point's x
     * @param z       the point's z
     */
    public static String actionLine(String oreName, Deposit deposit, int x, int z) {
        return "&7Nearest &f" + oreName + "&7: &f" + place(deposit, x, z) + "&7, " + RichnessTier.of(deposit.remaining()).coloured()
                + "&7, &f" + DrillText.amount(deposit.remaining()) + " &7left";
    }

    /**
     * A chat line of the nearest deposits: {@code Galena Ore: 42 m NE, Rich, 3.2M left (64%)}, the share being what is left of
     * what the deposit started with.
     *
     * @param oreName the ore's name, with colour codes
     * @param deposit the deposit
     * @param x       the point's x
     * @param z       the point's z
     */
    public static String listLine(String oreName, Deposit deposit, int x, int z) {
        return "&8 - &f" + oreName + "&7: &f" + place(deposit, x, z) + "&7, " + RichnessTier.of(deposit.remaining()).coloured()
                + "&7, &f" + DrillText.amount(deposit.remaining()) + " &7left &8(" + DrillText.share(deposit) + ")";
    }

    /**
     * A chat line of a survey: the exact centre and radius of a deposit, for a player who wants to find it on foot.
     *
     * @param oreName the ore's name, with colour codes
     * @param deposit the deposit
     */
    public static String surveyLine(String oreName, Deposit deposit) {
        return "&8 - &f" + oreName + "&7: centre &f" + deposit.centreX() + ", " + deposit.centreZ() + "&7, radius &f"
                + deposit.radius() + " &7blocks";
    }
}
