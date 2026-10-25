package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryMagazine;
import io.corefactory.core.artillery.FiringVerdict;

/** The words a player reads when a shot is refused: one short line for each verdict of the firing rules and of the magazine. */
public final class ArtilleryVerdicts {

    private ArtilleryVerdicts() {
    }

    /**
     * The line a refused shot shows the player when its magazine cannot fire it.
     *
     * @param verdict the verdict of the magazine's check; {@link ArtilleryMagazine.Verdict#OK} gives an empty line
     * @return the message, with its colour codes
     */
    public static String message(ArtilleryMagazine.Verdict verdict) {
        return switch (verdict) {
            case OK -> "";
            case FULL -> "&cThe magazine is full.";
            case NO_SHELL -> "&cThe magazine has no shell: load a shell and a charge.";
            case NO_CHARGE -> "&cThe magazine has no charge: load a shell and a charge.";
            case EMPTY -> "&cThe magazine is empty: load a shell and a charge.";
        };
    }

    /**
     * The line a refused shot or strike shows when a piece stands in a chunk that is not loaded. A chunk the plugin holds loads again by itself;
     * one it does not hold loads when a player walks near it.
     *
     * @param held true when the plugin holds the piece's chunk
     * @param x    the x of the piece's block
     * @param y    the y of the piece's block
     * @param z    the z of the piece's block
     * @return the message, with its colour codes
     */
    public static String pieceNotLoaded(boolean held, int x, int y, int z) {
        String where = "&cThe artillery piece at " + x + ", " + y + ", " + z + " is in an unloaded chunk; ";
        return where + (held ? "it loads again in a few seconds, or walk near it." : "walk near it to load it.");
    }

    /**
     * The line a refused shot or strike shows when the aim's chunk is not loaded. A chunk in the target area loads again by itself, so the
     * player is asked to wait; a chunk the plugin does not hold is one a player must walk near.
     *
     * @param held true when the plugin holds the aim's chunk, as the target area does while the target is set
     * @param x    the x of the aim, floored to a block
     * @param z    the z of the aim, floored to a block
     * @return the message, with its colour codes
     */
    public static String aimNotLoaded(boolean held, int x, int z) {
        if (held) {
            return "&cThe target area is loading; try again in a few seconds.";
        }
        return "&cThe target area at " + x + ", " + z + " is not loaded; walk near it to load it.";
    }

    /**
     * The line a refused shot shows the player.
     *
     * @param verdict the verdict of the firing rules; {@link FiringVerdict#OK} gives an empty line
     * @return the message, with its colour codes
     */
    public static String message(FiringVerdict verdict) {
        return switch (verdict) {
            case OK -> "";
            case CHARGE_TOO_LOW -> "&cThe charge is too weak for this shell. Load a higher tier.";
            case LEVEL_TOO_LOW -> "&cThis piece is too small for this shell. It needs a higher level.";
            case ELEVATION_TOO_LOW -> "&cThe barrel is too low. Raise the elevation.";
            case ELEVATION_TOO_HIGH -> "&cThe barrel is too high. Lower the elevation.";
            case CLUSTER_ELEVATION_TOO_LOW -> "&cThis shell needs a steeper elevation, so its bombs fall from above.";
            case OUT_OF_RANGE -> "&cThat spot is beyond the range of this piece.";
            case TOO_CLOSE -> "&cThat spot is too close for this shell.";
            case BLAST_REACHES_ARTILLERY -> "&cThe blast would reach the piece. Aim further away.";
            case BLAST_REACHES_PLAYER -> "&cThe blast would reach you. Aim further away.";
            case NUCLEAR_NEAR_PLAYER -> "&cA nuclear shell may not land this near a player.";
            case COOLDOWN_ACTIVE -> "&cAntimatter shells are on cooldown for you.";
            case POWER_SHORT -> "&cThe piece does not have its full power.";
        };
    }
}
