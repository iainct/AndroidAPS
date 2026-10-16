package app.aaps.wear.watchfaces.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * The rule behind the `showIn` key: in which mode a visible view is drawn.
 */
class ModeVisibilityTest {

    @Test
    fun `a view without showIn is drawn in both modes`() {
        assertThat(shownInMode(showIn = null, interactive = true)).isTrue()
        assertThat(shownInMode(showIn = null, interactive = false)).isTrue()
        assertThat(shownInMode(showIn = "", interactive = false)).isTrue()
        assertThat(shownInMode(showIn = "always", interactive = false)).isTrue()
    }

    @Test
    fun `an unknown value is treated as always`() {
        // A typo in a zip must not make a view disappear
        assertThat(shownInMode(showIn = "sometimes", interactive = true)).isTrue()
        assertThat(shownInMode(showIn = "sometimes", interactive = false)).isTrue()
    }

    @Test
    fun `interactive is drawn only while awake`() {
        assertThat(shownInMode(showIn = "interactive", interactive = true)).isTrue()
        assertThat(shownInMode(showIn = "interactive", interactive = false)).isFalse()
    }

    @Test
    fun `ambient is drawn only in always-on`() {
        assertThat(shownInMode(showIn = "ambient", interactive = true)).isFalse()
        assertThat(shownInMode(showIn = "ambient", interactive = false)).isTrue()
    }

    @Test
    fun `gives the same answer however often it is asked`() {
        val answers = (1..10).map { shownInMode(showIn = "ambient", interactive = false) }

        assertThat(answers.toSet()).containsExactly(true)
    }

    @Test
    fun `a design depends on the mode only when a view says interactive or ambient`() {
        assertThat(dependsOnMode(listOf(null, "", "always"))).isFalse()
        assertThat(dependsOnMode(listOf(null, "ambient"))).isTrue()
        assertThat(dependsOnMode(listOf("interactive"))).isTrue()
        assertThat(dependsOnMode(emptyList())).isFalse()
    }
}
