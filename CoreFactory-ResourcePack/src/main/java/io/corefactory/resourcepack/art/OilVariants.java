package io.corefactory.resourcepack.art;

import java.util.List;

/**
 * The oil tubes: the water pipe re-skinned. An oil tube has the shapes, the orientations and the painters of a water pipe, so its
 * models are baked from the water geometry by {@link WaterVariants} in the oil palette: a dark body, orange accents, a light orange
 * band across the top, and a dark bore. Nothing is copied. The models are the water pipe models without the pumps, with the oil
 * prefix, in the water order. The ids are those of {@code OilModels.all()} in CoreFactory-Core.
 *
 * <p>The three item sprites are the water pipe's sprites, the T's and the cross's, painted in the oil palette. An oil tube is not a
 * machine and has no pump, so there is no model of a pump here.
 */
public final class OilVariants {

    /** The oil palette: a dark body, an orange accent, a lighter orange band, and a dark bore. */
    static final WaterArt.Palette PALETTE = new WaterArt.Palette("#2B2724", "#E8731E", "#FFA04A", "#0B0907");

    /** Every oil tube model, in the order of OilModels.all(). */
    public static final List<MachineArt.Spec> ALL = WaterVariants.bake(PALETTE,
            id -> !id.startsWith("water_pump"), OilVariants::oilId);

    private static final List<String> IDS = ALL.stream().map(MachineArt.Spec::id).toList();

    /** The item sprites: the pipe, the T and the cross, in the oil palette. */
    public static final List<ComponentArt.Spec> SPRITES = List.of(
            new ComponentArt.Spec("oil_pipe", PALETTE.base(), PALETTE.accent(), WaterArt::pipeSprite),
            new ComponentArt.Spec("oil_splitter_t", PALETTE.base(), PALETTE.accent(), WaterArt::tSprite),
            new ComponentArt.Spec("oil_splitter_x", PALETTE.base(), PALETTE.accent(), WaterArt::crossSprite));

    private OilVariants() {
    }

    /** Every oil tube model id, in the order of OilModels.all(). */
    public static List<String> ids() {
        return IDS;
    }

    /** The oil id of a water pipe model: the same shape with the oil prefix. */
    static String oilId(String waterId) {
        if (!waterId.startsWith("water_")) {
            throw new IllegalArgumentException("not a water pipe model: " + waterId);
        }
        return "oil_" + waterId.substring("water_".length());
    }
}
