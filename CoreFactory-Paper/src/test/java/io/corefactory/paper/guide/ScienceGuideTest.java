package io.corefactory.paper.guide;

import io.corefactory.paper.ShippedContent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScienceGuideTest {

    private static ShippedContent.Loaded data;
    private static List<ScienceGuide.Chapter> chapters;

    @BeforeAll
    static void build() {
        data = ShippedContent.load();
        chapters = ScienceGuide.chapters(new ScienceGuide.Source(data.tree(), data.processes(), data.ores(),
                data.definitions().tnts().values(), data.definitions().components()));
    }

    private static String text(String title) {
        return chapters.stream().filter(chapter -> chapter.title().equals(title)).findFirst()
                .orElseThrow().paragraphs().toString();
    }

    @Test
    void thereAreEightChaptersInTheOrderOfTheGuide() {
        assertThat(chapters).extracting(ScienceGuide.Chapter::title).containsExactly(
                "1. Find the ore", "2. The uranium chain", "3. Thorium, U-233 and plutonium", "4. Hydrogen and particles",
                "5. Exotic matter", "6. Research", "7. Warheads", "8. Machines");
    }

    @Test
    void eachChapterStartsWithItsTitle() {
        assertThat(chapters).allSatisfy(chapter -> assertThat(chapter.paragraphs().get(0))
                .isEqualTo("&6&l" + chapter.title()));
    }

    @Test
    void theOreChapterGivesEachOreItsDepthAndDropFromTheFile() {
        String ore = text("1. Find the ore");

        assertThat(ore).contains("Uraninite Ore").contains("Found from y -48 to y 24")
                .contains("Mining gives 2 to 4 Raw Uraninite.");
    }

    @Test
    void theRecipesAreRenderedFromTheProcessFile() {
        String uranium = text("2. The uranium chain");

        assertThat(uranium).contains("1 Raw Uraninite -> 2 Crushed Uraninite, 5 s.");
        assertThat(uranium).contains("2 Crushed Uraninite -> 1 Uranium Calcine, 1 Slag (40%), 10 s, uses one fuel.");
        assertThat(uranium).contains("8 UF6 Cylinder -> 1 Enriched Uranium, 2 Depleted Uranium, 60 s.");
    }

    @Test
    void theWaterRecipeIsTakenFromTheFileNotFromTheDocument() {
        assertThat(text("4. Hydrogen and particles"))
                .contains("2 Water Bucket -> 1 Deuterium Cell, 2 Bucket, 20 s.");
    }

    @Test
    void theRecipesOfComponentsAreCountedFromTheirPatterns() {
        assertThat(text("5. Exotic matter")).contains("4 Superconducting Magnet, 4 Antiproton Capsule, 1 High-Purity Graphite");
        assertThat(text("3. Thorium, U-233 and plutonium"))
                .contains("4 Plutonium Oxide, 1 Nether Star, 1 Gold Block");
    }

    @Test
    void theCondenserTimeComesFromItsTicks() {
        assertThat(text("4. Hydrogen and particles")).contains("120 s");
    }

    @Test
    void theResearchChapterListsEveryNodeWithItsCostFromTheTree() {
        String research = text("6. Research");

        for (var node : data.tree().nodes()) {
            assertThat(research).contains(ScienceGuide.plain(node.name()));
        }
        assertThat(research).contains("Cost: 8 Research Pack: Chemical, 8 Research Pack: Nuclear.");
        assertThat(research).contains("Cost: 5 Research Pack: Basic.");
    }

    @Test
    void theResearchNodesAreListedInTheOrderOfTheLine() {
        String research = text("6. Research");
        List<String> order = List.of("Laboratory Basics", "Radiation Safety", "Metallurgy", "Chemical Separation",
                "Fluorine Chemistry", "Nuclear Fission", "Hydrogen Processes", "Materials Purification",
                "Plasma Physics", "Thermonuclear Engineering", "Isotope Transmutation", "Particle Physics",
                "Antimatter Physics", "Exotic Matter", "Tesseract Geometry");
        int previous = -1;
        for (int number = 1; number <= order.size(); number++) {
            int at = research.indexOf(number + ". " + order.get(number - 1));
            assertThat(at).as("node " + number + " " + order.get(number - 1)).isGreaterThan(previous);
            previous = at;
        }
    }

    @Test
    void theWarheadsNameTheirNodeAndTheirYield() {
        String warheads = text("7. Warheads");

        assertThat(warheads).contains("Research: Nuclear Fission");
        assertThat(warheads).contains("Yield 20 t.");
        assertThat(warheads).contains("Yield 5 kt.");
        assertThat(warheads).contains("Yield 1000 Mt.");
        assertThat(warheads).contains("Research: Tesseract Geometry");
    }

    @Test
    void theMachinesChapterGivesEachMachineItsSlotsAndHost() {
        String machines = text("8. Machines");

        assertThat(machines).contains("Roasting Kiln").contains("Inputs 2, outputs 3, fuel 1. Placed on Barrel.");
    }

    @Test
    void yieldsAreWrittenInTheLargestUnitThatStaysAtOrAboveOne() {
        assertThat(ScienceGuide.yieldText(0.00002)).isEqualTo("20 t");
        assertThat(ScienceGuide.yieldText(0.005)).isEqualTo("5 kt");
        assertThat(ScienceGuide.yieldText(0.015)).isEqualTo("15 kt");
        assertThat(ScienceGuide.yieldText(0.5)).isEqualTo("0.5 Mt");
        assertThat(ScienceGuide.yieldText(15)).isEqualTo("15 Mt");
        assertThat(ScienceGuide.yieldText(1000)).isEqualTo("1000 Mt");
    }

    @Test
    void timesAreWrittenInSeconds() {
        assertThat(ScienceGuide.seconds(100)).isEqualTo("5 s");
        assertThat(ScienceGuide.seconds(240)).isEqualTo("12 s");
        assertThat(ScienceGuide.seconds(250)).isEqualTo("12.5 s");
    }

    @Test
    void theShippedGuideFitsInOneWrittenBook() {
        int pages = chapters.stream().mapToInt(chapter -> PageSplitter.paginate(chapter.paragraphs()).size()).sum();

        assertThat(pages + 8 + 2).isLessThanOrEqualTo(GuideBook.MAX_PAGES);
    }
}
