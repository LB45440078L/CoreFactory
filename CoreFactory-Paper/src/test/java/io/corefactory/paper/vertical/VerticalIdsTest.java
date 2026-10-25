package io.corefactory.paper.vertical;

import io.corefactory.core.item.ItemFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerticalIdsTest {

    @Test
    void theComponentIdNamesTheLevel() {
        assertThat(VerticalIds.componentId(1)).isEqualTo("vertical_belt_1");
        assertThat(VerticalIds.componentId(3)).isEqualTo("vertical_belt_3");
    }

    @Test
    void aLevelOutsideOneToThreeHasNoId() {
        assertThatThrownBy(() -> VerticalIds.componentId(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VerticalIds.componentId(4)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aVerticalBeltIdReadsBackItsLevel() {
        assertThat(VerticalIds.levelOf("vertical_belt_2").isPresent()).isTrue();
        assertThat(VerticalIds.levelOf("vertical_belt_2").getAsInt()).isEqualTo(2);
    }

    @Test
    void anotherIdIsNoVerticalBelt() {
        assertThat(VerticalIds.levelOf("vertical_belt_4").isPresent()).isFalse();
        assertThat(VerticalIds.levelOf("vertical_belt").isPresent()).isFalse();
        assertThat(VerticalIds.levelOf("belt_1").isPresent()).isFalse();
        assertThat(VerticalIds.levelOf("vertical_belt_12").isPresent()).isFalse();
        assertThat(VerticalIds.levelOf(null).isPresent()).isFalse();
    }

    @Test
    void theRomanNumeralsAreIIIIII() {
        assertThat(VerticalIds.roman(1)).isEqualTo("I");
        assertThat(VerticalIds.roman(2)).isEqualTo("II");
        assertThat(VerticalIds.roman(3)).isEqualTo("III");
    }

    @Test
    void eachLevelShowsItsMachineModel() {
        for (int level = 1; level <= VerticalIds.MAX_LEVEL; level++) {
            String id = VerticalIds.componentId(level);
            assertThat(ItemFactory.displayKey(id)).isEqualTo(ItemFactory.MACHINE + "/" + id);
        }
    }
}
