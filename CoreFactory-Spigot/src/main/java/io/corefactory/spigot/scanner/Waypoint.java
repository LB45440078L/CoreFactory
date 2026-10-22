package io.corefactory.spigot.scanner;

import io.corefactory.core.deposit.Deposit;

/**
 * One player's waypoint: the deposit it leads to, the ore's name with colour codes, and the server tick it expires at. A player
 * has at most one, and reaching the deposit or the expiry ends it.
 *
 * @param deposit     the deposit the waypoint leads to, as it was when set
 * @param name        the ore's name, with colour codes
 * @param expiresTick the server tick at which the waypoint expires
 */
public record Waypoint(Deposit deposit, String name, long expiresTick) {
}
