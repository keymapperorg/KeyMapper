package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.common.utils.PointKM
import io.github.sds100.keymapper.common.utils.SizeKM
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

/**
 * Tests for the coordinate scaling in issue #2217.
 */
class ScreenCoordinateScalingTest {

    companion object {
        private val PORTRAIT_1080 = SizeKM(1080, 2400)
        private val PORTRAIT_1440 = SizeKM(1440, 3200)
        private val LANDSCAPE_1080 = SizeKM(2400, 1080)
        private val LANDSCAPE_1440 = SizeKM(3200, 1440)
    }

    @Test
    fun `dont scale coordinate when the resolution is unknown`() {
        // GIVEN an action that was created before the resolution was saved

        // WHEN
        val point = scaleCoordinate(x = 540, y = 1200, from = null, to = PORTRAIT_1440)

        // THEN
        assertThat(point, `is`(PointKM(540, 1200)))
    }

    @Test
    fun `dont scale coordinate when the resolution has not changed`() {
        // WHEN
        val point = scaleCoordinate(x = 540, y = 1200, from = PORTRAIT_1080, to = PORTRAIT_1080)

        // THEN
        assertThat(point, `is`(PointKM(540, 1200)))
    }

    @Test
    fun `scale coordinate up to a higher resolution`() {
        // WHEN 1080x2400 -> 1440x3200 is a factor of 4 3 on both axes
        val point = scaleCoordinate(x = 540, y = 1200, from = PORTRAIT_1080, to = PORTRAIT_1440)

        // THEN
        assertThat(point, `is`(PointKM(720, 1600)))
    }

    @Test
    fun `scale coordinate down to a lower resolution`() {
        // WHEN
        val point = scaleCoordinate(x = 720, y = 1600, from = PORTRAIT_1440, to = PORTRAIT_1080)

        // THEN
        assertThat(point, `is`(PointKM(540, 1200)))
    }

    @Test
    fun `dont scale coordinate when only the orientation is different`() {
        // GIVEN the coordinate was picked from a portrait screenshot and the display is now
        // landscape at the same resolution.

        // WHEN
        val point = scaleCoordinate(x = 540, y = 1200, from = PORTRAIT_1080, to = LANDSCAPE_1080)

        // THEN the saved size is normalised to landscape first so nothing moves.
        assertThat(point, `is`(PointKM(540, 1200)))
    }

    @Test
    fun `scale coordinate when the orientation and the resolution are both different`() {
        // WHEN the saved portrait size normalises to 2400x1080, which is a factor of 4 3 to
        // 3200x1440.
        val point = scaleCoordinate(x = 540, y = 1200, from = PORTRAIT_1080, to = LANDSCAPE_1440)

        // THEN
        assertThat(point, `is`(PointKM(720, 1600)))
    }

    @Test
    fun `scale coordinate saved in landscape to a portrait display of another resolution`() {
        // WHEN the saved landscape size normalises to 1080x2400, which is a factor of 4 3 to
        // 1440x3200.
        val point = scaleCoordinate(x = 540, y = 1200, from = LANDSCAPE_1080, to = PORTRAIT_1440)

        // THEN
        assertThat(point, `is`(PointKM(720, 1600)))
    }

    @Test
    fun `round the scaled coordinate to the nearest pixel`() {
        // WHEN 100 -> 150 is a factor of 1 5 so 5 scales to 7 5
        val point = scaleCoordinate(x = 5, y = 5, from = SizeKM(100, 100), to = SizeKM(150, 150))

        // THEN
        assertThat(point, `is`(PointKM(8, 8)))
    }

    @Test
    fun `dont scale coordinate when a display size is invalid`() {
        // WHEN
        val zeroSource =
            scaleCoordinate(x = 540, y = 1200, from = SizeKM(0, 2400), to = PORTRAIT_1440)
        val negativeSource =
            scaleCoordinate(x = 540, y = 1200, from = SizeKM(1080, -1), to = PORTRAIT_1440)
        val zeroTarget =
            scaleCoordinate(x = 540, y = 1200, from = PORTRAIT_1080, to = SizeKM(1440, 0))

        // THEN
        assertThat(zeroSource, `is`(PointKM(540, 1200)))
        assertThat(negativeSource, `is`(PointKM(540, 1200)))
        assertThat(zeroTarget, `is`(PointKM(540, 1200)))
    }

    @Test
    fun `dont scale distance when the resolution is unknown`() {
        // WHEN
        val distance = scaleDistance(distance = 300, from = null, to = PORTRAIT_1440)

        // THEN
        assertThat(distance, `is`(300))
    }

    @Test
    fun `dont scale distance when the resolution has not changed`() {
        // WHEN
        val distance = scaleDistance(distance = 300, from = PORTRAIT_1080, to = PORTRAIT_1080)

        // THEN
        assertThat(distance, `is`(300))
    }

    @Test
    fun `scale distance when both axes scale equally`() {
        // WHEN
        val distance = scaleDistance(distance = 300, from = PORTRAIT_1080, to = PORTRAIT_1440)

        // THEN
        assertThat(distance, `is`(400))
    }

    @Test
    fun `scale distance by the average ratio when the axes scale differently`() {
        // GIVEN the width doubles and the height stays the same, so the average ratio is 1 5.

        // WHEN
        val distance =
            scaleDistance(distance = 300, from = SizeKM(1000, 2000), to = SizeKM(2000, 2000))

        // THEN
        assertThat(distance, `is`(450))
    }

    @Test
    fun `dont scale distance when only the orientation is different`() {
        // WHEN
        val distance = scaleDistance(distance = 300, from = PORTRAIT_1080, to = LANDSCAPE_1080)

        // THEN
        assertThat(distance, `is`(300))
    }

    @Test
    fun `dont scale distance when a display size is invalid`() {
        // WHEN
        val distance = scaleDistance(distance = 300, from = SizeKM(1080, 0), to = PORTRAIT_1440)

        // THEN
        assertThat(distance, `is`(300))
    }
}
