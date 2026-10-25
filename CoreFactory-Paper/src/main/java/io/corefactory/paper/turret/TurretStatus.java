package io.corefactory.paper.turret;

/** What a turret is doing now, as its hologram and menu show it. */
public enum TurretStatus {

    NO_ROUNDS("No rounds"),
    NO_POWER("No power"),
    OFF("Off"),
    IDLE("Idle"),
    ENGAGING("Engaging");

    private final String text;

    TurretStatus(String text) {
        this.text = text;
    }

    /** The word shown for the status, without colour codes. */
    public String text() {
        return text;
    }
}
