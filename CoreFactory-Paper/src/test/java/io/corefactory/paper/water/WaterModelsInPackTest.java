package io.corefactory.paper.water;

import io.corefactory.core.water.WaterModels;
import io.corefactory.resourcepack.art.WaterVariants;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The pack generates exactly the models the plugin asks for, in the same order. */
class WaterModelsInPackTest {

    @Test
    void thePackHasEveryWaterModelTheCodeNames() {
        assertThat(WaterVariants.ids()).containsExactlyElementsOf(WaterModels.all());
    }
}
