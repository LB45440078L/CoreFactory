package io.corefactory.core.geology;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeologyServiceTest {

    private static OreDefinition ore(String id) {
        return new OreDefinition(id, id, "SMOOTH_BASALT", "raw_" + id, 1, 2, Set.of("STONE"), -32, 16);
    }

    private static GeologyService three() {
        return new GeologyService(List.of(ore("first"), ore("second"), ore("third")));
    }

    @Test
    void indexOfFindsAnOreByIdAndSaysMinusOneForAnUnknownId() {
        GeologyService service = three();

        assertThat(service.indexOf("first")).isZero();
        assertThat(service.indexOf("second")).isEqualTo(1);
        assertThat(service.indexOf("fourth")).isEqualTo(-1);
        assertThat(service.indexOf("First")).isEqualTo(-1);
    }

    @Test
    void oresKeepTheirFileOrder() {
        GeologyService service = three();

        assertThat(service.ores()).extracting(OreDefinition::id).containsExactly("first", "second", "third");
        assertThat(service.ore(2).id()).isEqualTo("third");
    }

    @Test
    void tooManyOresAreRefused() {
        List<OreDefinition> many = Collections.nCopies(257, ore("one"));

        assertThatThrownBy(() -> new GeologyService(many)).isInstanceOf(IllegalArgumentException.class);
    }
}
