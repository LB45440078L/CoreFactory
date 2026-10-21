package io.corefactory.core.engine;

import io.corefactory.core.engine.VisualCatalog.Effect;
import io.corefactory.core.engine.VisualCatalog.Placement;
import io.corefactory.core.engine.VisualCatalog.Shape;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class VisualCatalogTest {

    private static final List<String> PACK_SPRITES = List.of(
            "fireball_core", "fireball_ember", "shock_ring", "smoke_puff", "steam_puff", "dust_cloud",
            "neutron_spark", "radiation_trefoil", "antimatter_flare", "tesseract_shard", "research_spark",
            "geiger_tick", "crystal_glint", "machine_steam", "fluorine_fume");

    private static final List<String> EFFECT_NAMES = List.of(
            "nuclear_fireball", "nuclear_shockwave", "nuclear_surge", "nuclear_mushroom", "nuclear_fallout",
            "neutron_burst", "antimatter_flash", "tesseract_fold", "machine_work", "research_complete", "ore_break",
            "geiger_crackle", "lab_crusher", "lab_roaster", "lab_separator", "lab_purifier", "lab_converter",
            "lab_enrichment", "lab_transmuter", "lab_spectrometer", "lab_accelerator", "lab_condenser");

    /** Each lab effect's placement count and lifetime in ticks: the pure geometry the lab visuals are checked against. */
    private static final Map<String, int[]> LAB_TABLE = Map.of(
            "lab_crusher", new int[]{24, 30},
            "lab_roaster", new int[]{18, 50},
            "lab_separator", new int[]{20, 50},
            "lab_purifier", new int[]{12, 60},
            "lab_converter", new int[]{16, 60},
            "lab_enrichment", new int[]{24, 60},
            "lab_transmuter", new int[]{12, 30},
            "lab_spectrometer", new int[]{11, 40},
            "lab_accelerator", new int[]{16, 40},
            "lab_condenser", new int[]{16, 40});

    @Test
    void spritesAreTheFifteenPackSprites() {
        assertThat(VisualCatalog.SPRITES).containsExactlyInAnyOrderElementsOf(PACK_SPRITES);
    }

    @Test
    void everyNamedEffectIsListedOnce() {
        assertThat(VisualCatalog.EFFECTS).extracting(Effect::name).containsExactlyElementsOf(EFFECT_NAMES);
        for (String name : EFFECT_NAMES) {
            assertThat(VisualCatalog.find(name)).isPresent();
        }
        assertThat(VisualCatalog.find("no_such_effect")).isEmpty();
    }

    @Test
    void everySpriteAnEffectUsesIsAPackSprite() {
        for (Effect effect : VisualCatalog.EFFECTS) {
            assertThat(effect.sprites()).isNotEmpty().allSatisfy(id -> assertThat(PACK_SPRITES).contains(id));
            for (Placement placement : VisualCatalog.placements(effect, 12.0)) {
                assertThat(PACK_SPRITES).as(effect.name()).contains(placement.sprite());
            }
        }
    }

    @Test
    void countsMatchTheShape() {
        for (Shape shape : Shape.values()) {
            int expected = shape == Shape.FLASH ? 8 : 7;
            assertThat(shape.placements(7, 5.0, List.of("crystal_glint", "fireball_core"), 40))
                    .as(shape.name())
                    .hasSize(expected);
        }
        assertThat(Shape.FLASH.placements(24, 9.0, List.of("antimatter_flare", "crystal_glint"), 60)).hasSize(25);
    }

    @Test
    void effectsHaveTheCountTheCatalogGivesThem() {
        for (Effect effect : VisualCatalog.EFFECTS) {
            int expected = effect.shape() == Shape.FLASH ? effect.count() + 1 : effect.count();
            assertThat(VisualCatalog.placements(effect, 20.0)).as(effect.name()).hasSize(expected);
        }
    }

    @Test
    void everyPlacementEndsByTheEffectsLifetime() {
        for (Effect effect : VisualCatalog.EFFECTS) {
            for (Placement placement : VisualCatalog.placements(effect, 30.0)) {
                assertThat(placement.delay()).as(effect.name()).isGreaterThanOrEqualTo(0);
                assertThat(placement.lifetime()).as(effect.name()).isGreaterThanOrEqualTo(1);
                assertThat(placement.delay() + placement.lifetime()).as(effect.name())
                        .isLessThanOrEqualTo(effect.lifetime());
                assertThat(placement.startScale()).as(effect.name()).isPositive();
                assertThat(placement.endScale()).as(effect.name()).isPositive();
            }
        }
    }

    @Test
    void sphereBurstPointsLieAtTheRequestedRadius() {
        List<Placement> burst = Shape.SPHERE_BURST.placements(40, 7.5, List.of("fireball_core"), 60);

        assertThat(burst).allSatisfy(p -> assertThat(p.offset().length()).isCloseTo(7.5, within(1e-9)));
        assertThat(burst).allSatisfy(p -> assertThat(p.from().length()).isZero());
        assertThat(burst).anySatisfy(p -> assertThat(p.offset().getY()).isPositive());
        assertThat(burst).anySatisfy(p -> assertThat(p.offset().getY()).isNegative());
    }

    @Test
    void sphereBurstPointsAreDistinct() {
        List<Placement> burst = Shape.SPHERE_BURST.placements(30, 10.0, List.of("neutron_spark"), 40);

        for (int i = 0; i < burst.size(); i++) {
            for (int j = i + 1; j < burst.size(); j++) {
                assertThat(burst.get(i).offset().distance(burst.get(j).offset())).isGreaterThan(1e-6);
            }
        }
    }

    @Test
    void ringLiesOnTheGroundPlaneAtTheRadius() {
        List<Placement> ring = Shape.RING.placements(16, 9.0, List.of("shock_ring"), 40);

        assertThat(ring).allSatisfy(p -> assertThat(p.offset().getY()).isZero());
        assertThat(ring).allSatisfy(p -> assertThat(Math.hypot(p.offset().getX(), p.offset().getZ()))
                .isCloseTo(9.0, within(1e-9)));
        assertThat(ring).allSatisfy(p -> assertThat(p.from().length()).isLessThan(p.offset().length()));
    }

    @Test
    void columnRisesFromTheGroundWithACapAtTheTop() {
        double height = 20.0;
        List<Placement> column = Shape.COLUMN.placements(30, height, List.of("dust_cloud", "smoke_puff", "steam_puff"),
                200);

        assertThat(column).allSatisfy(p -> assertThat(p.offset().getY()).isBetween(0.0, height));
        Placement highest = column.stream().max((a, b) -> Double.compare(a.offset().getY(), b.offset().getY()))
                .orElseThrow();
        assertThat(highest.sprite()).isEqualTo("steam_puff");
        assertThat(highest.offset().getY()).isGreaterThan(height * 0.8);
    }

    @Test
    void mushroomStemRisesOverAboutThreeSeconds() {
        Effect mushroom = VisualCatalog.find("nuclear_mushroom").orElseThrow();
        List<Placement> stem = VisualCatalog.placements(mushroom, 60.0).stream()
                .filter(p -> p.offset().getY() < 0.7 * 60.0).toList();

        assertThat(stem).isNotEmpty();
        assertThat(stem).allSatisfy(p -> assertThat(p.sprite()).isIn("dust_cloud", "smoke_puff"));
        assertThat(stem.stream().mapToInt(Placement::delay).max().orElseThrow()).isBetween(50, 60);
        assertThat(stem).allSatisfy(p -> assertThat(p.from().getY()).isZero());
    }

    @Test
    void mushroomCapIsDarkAtTheRimAndBrightAtTheCrown() {
        Effect mushroom = VisualCatalog.find("nuclear_mushroom").orElseThrow();
        List<Placement> cap = VisualCatalog.placements(mushroom, 60.0).stream()
                .filter(p -> p.offset().getY() >= 0.7 * 60.0).toList();
        Placement rim = cap.stream().max((a, b) -> Double.compare(
                Math.hypot(a.offset().getX(), a.offset().getZ()), Math.hypot(b.offset().getX(), b.offset().getZ())))
                .orElseThrow();

        assertThat(rim.sprite()).isEqualTo("dust_cloud");
        assertThat(cap).anySatisfy(p -> assertThat(p.sprite()).isEqualTo("steam_puff"));
        assertThat(cap).allSatisfy(p -> assertThat(p.delay()).isGreaterThanOrEqualTo(30));
    }

    @Test
    void mushroomCapSpreadsOutwardOverTime() {
        Effect mushroom = VisualCatalog.find("nuclear_mushroom").orElseThrow();
        List<Placement> cap = VisualCatalog.placements(mushroom, 60.0).stream()
                .filter(p -> p.offset().getY() >= 0.7 * 60.0).toList();

        Placement inner = cap.stream().min((a, b) -> Double.compare(
                Math.hypot(a.offset().getX(), a.offset().getZ()), Math.hypot(b.offset().getX(), b.offset().getZ())))
                .orElseThrow();
        Placement outer = cap.stream().max((a, b) -> Double.compare(
                Math.hypot(a.offset().getX(), a.offset().getZ()), Math.hypot(b.offset().getX(), b.offset().getZ())))
                .orElseThrow();
        assertThat(outer.delay()).isGreaterThan(inner.delay());
    }

    @Test
    void mushroomSpriteCountGrowsWithItsHeightToThreeTimesTheBase() {
        Effect mushroom = VisualCatalog.find("nuclear_mushroom").orElseThrow();

        assertThat(VisualCatalog.placements(mushroom, 17.0)).hasSize(40);
        assertThat(VisualCatalog.placements(mushroom, 52.0)).hasSize(80);
        assertThat(VisualCatalog.placements(mushroom, 78.0)).hasSize(120);
        assertThat(VisualCatalog.placements(mushroom, 400.0)).hasSize(120);
    }

    @Test
    void mushroomEveryPuffShrinksOutAtTheEnd() {
        Effect mushroom = VisualCatalog.find("nuclear_mushroom").orElseThrow();

        assertThat(VisualCatalog.placements(mushroom, 60.0)).allSatisfy(p -> {
            assertThat(p.fade()).isPositive().isLessThan(p.lifetime());
            assertThat(p.delay() + p.lifetime()).isEqualTo(mushroom.lifetime());
        });
    }

    @Test
    void groundSurgeRollsOutOnTheGroundAndSpreadsItsSpawns() {
        Effect surge = VisualCatalog.find("nuclear_surge").orElseThrow();
        List<Placement> ring = VisualCatalog.placements(surge, 150.0);

        assertThat(ring).hasSize(64);
        assertThat(ring).allSatisfy(p -> assertThat(p.sprite()).isEqualTo("dust_cloud"));
        assertThat(ring).allSatisfy(p -> assertThat(p.offset().getY()).isZero());
        assertThat(ring).anySatisfy(p -> assertThat(p.delay()).isGreaterThan(0));
        assertThat(ring.stream().mapToInt(Placement::delay).max().orElseThrow()).isLessThanOrEqualTo(10);
    }

    @Test
    void labEffectsHaveTheirCountsSpritesAndLifetimes() {
        for (Map.Entry<String, int[]> row : LAB_TABLE.entrySet()) {
            Effect effect = VisualCatalog.find(row.getKey()).orElseThrow();
            List<Placement> placements = VisualCatalog.placements(effect, 2.5);

            assertThat(placements).as(effect.name()).hasSize(row.getValue()[0]);
            assertThat(effect.lifetime()).as(effect.name()).isEqualTo(row.getValue()[1]);
            assertThat(placements).as(effect.name()).allSatisfy(p -> {
                assertThat(VisualCatalog.SPRITES).contains(p.sprite());
                assertThat(p.delay() + p.lifetime()).isLessThanOrEqualTo(effect.lifetime());
            });
        }
    }

    @Test
    void labEffectsUseTheSpritesTheirMachinesNeed() {
        assertThat(VisualCatalog.find("lab_converter").orElseThrow().sprites())
                .containsExactly("machine_steam", "fluorine_fume");
        assertThat(VisualCatalog.find("lab_spectrometer").orElseThrow().sprites())
                .containsExactly("research_spark", "geiger_tick");
        assertThat(VisualCatalog.find("lab_crusher").orElseThrow().sprites())
                .containsExactly("dust_cloud", "crystal_glint");
    }

    @Test
    void beamStreaksAlongALineThroughTheCentre() {
        List<Placement> beam = Shape.BEAM.placements(16, 6.0, List.of("neutron_spark"), 40);

        assertThat(beam).hasSize(16);
        assertThat(beam).allSatisfy(p -> assertThat(p.offset().getY()).isZero());
        assertThat(beam).allSatisfy(p -> assertThat(p.from().getX()).isCloseTo(-p.offset().getX(), within(1e-9)));
        assertThat(beam).allSatisfy(p -> assertThat(p.lifetime()).isLessThan(40));
        assertThat(beam.stream().mapToInt(Placement::delay).max().orElseThrow()).isGreaterThan(0);
    }

    @Test
    void flashGrowsThenSparksCollapseToTheCentre() {
        double scale = 9.0;
        List<Placement> flash = Shape.FLASH.placements(12, scale, List.of("antimatter_flare", "crystal_glint"), 60);

        Placement flare = flash.getFirst();
        assertThat(flare.sprite()).isEqualTo("antimatter_flare");
        assertThat(flare.offset().length()).isZero();
        assertThat(flare.endScale()).isGreaterThan(flare.startScale());

        List<Placement> sparks = flash.subList(1, flash.size());
        assertThat(sparks).allSatisfy(p -> assertThat(p.sprite()).isEqualTo("crystal_glint"));
        assertThat(sparks).allSatisfy(p -> assertThat(p.offset().length()).isZero());
        assertThat(sparks).allSatisfy(p -> assertThat(p.from().length()).isCloseTo(scale, within(1e-9)));
        assertThat(sparks).allSatisfy(p -> assertThat(p.delay()).isGreaterThan(flare.delay()));
    }

    @Test
    void burstIsAFlatStar() {
        List<Placement> star = Shape.BURST.placements(16, 4.0, List.of("research_spark"), 40);

        assertThat(star).allSatisfy(p -> assertThat(p.offset().getY()).isZero());
        assertThat(star).allSatisfy(p -> assertThat(p.from().length()).isZero());
        Vector reachOut = star.getFirst().offset();
        assertThat(reachOut.length()).isCloseTo(4.0, within(1e-9));
    }

    @Test
    void driftSpreadsOverTheRadiusAndDrifts() {
        List<Placement> drift = Shape.DRIFT.placements(40, 60.0, List.of("radiation_trefoil"), 100);

        assertThat(drift).allSatisfy(p -> assertThat(p.from().getY()).isPositive());
        assertThat(drift).allSatisfy(p -> assertThat(p.offset().distance(p.from())).isPositive());
        assertThat(drift).allSatisfy(p -> assertThat(Math.hypot(p.from().getX(), p.from().getZ()))
                .isLessThanOrEqualTo(60.0));
        assertThat(drift).allSatisfy(p -> assertThat(p.endScale()).isLessThan(p.startScale()));
    }

    @Test
    void spiralOrbitsOutwardFromNearTheCentre() {
        List<Placement> spiral = Shape.SPIRAL.placements(36, 50.0, List.of("tesseract_shard"), 100);

        assertThat(spiral).allSatisfy(p -> assertThat(Math.hypot(p.from().getX(), p.from().getZ()))
                .isCloseTo(10.0, within(1e-9)));
        assertThat(spiral).allSatisfy(p -> assertThat(Math.hypot(p.offset().getX(), p.offset().getZ()))
                .isGreaterThan(p.from().length()));
    }

    @Test
    void puffRisesInTurn() {
        List<Placement> puff = Shape.PUFF.placements(4, 1.0, List.of("machine_steam"), 30);

        assertThat(puff).allSatisfy(p -> assertThat(p.offset().getY()).isGreaterThan(p.from().getY()));
        assertThat(puff.getFirst().delay()).isZero();
        assertThat(puff.getLast().delay()).isGreaterThan(puff.getFirst().delay());
    }

    @Test
    void scaleSetsTheSizeOfTheEffect() {
        Effect fireball = VisualCatalog.find("nuclear_fireball").orElseThrow();
        double small = VisualCatalog.placements(fireball, 2.0).stream()
                .mapToDouble(p -> p.offset().length()).max().orElseThrow();
        double large = VisualCatalog.placements(fireball, 20.0).stream()
                .mapToDouble(p -> p.offset().length()).max().orElseThrow();
        assertThat(small).isCloseTo(2.0, within(1e-9));
        assertThat(large).isCloseTo(20.0, within(1e-9));
    }
}
