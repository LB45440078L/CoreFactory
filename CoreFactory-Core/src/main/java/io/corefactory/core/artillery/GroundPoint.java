package io.corefactory.core.artillery;

/**
 * A point on the ground plane, in blocks: a landing point, a submunition or a point of an outline.
 *
 * @param x the x coordinate, in blocks
 * @param z the z coordinate, in blocks
 */
public record GroundPoint(double x, double z) {
}
