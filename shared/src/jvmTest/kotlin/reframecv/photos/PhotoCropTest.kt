package reframecv.photos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhotoCropTest {
    @Test
    fun centersWithCoverScaleAndBoundsMovementWithoutExposingEmptyEdges() {
        val centered = PhotoCrop().placement(900f, 400f, 350f, 450f)
        assertEquals(1012.5f, centered.width)
        assertEquals(450f, centered.height)
        assertEquals(-331.25f, centered.left)
        assertEquals(0f, centered.top)
        listOf(1f, 2f, 4f).forEach { zoom ->
            listOf(-1f, 0f, 1f).forEach { x ->
                listOf(-1f, 0f, 1f).forEach { y ->
                    val placement = PhotoCrop(zoom, x, y).placement(900f, 400f, 350f, 450f)
                    assertTrue(placement.left <= 0)
                    assertTrue(placement.top <= 0)
                    assertTrue(placement.left + placement.width >= 350f)
                    assertTrue(placement.top + placement.height >= 450f)
                }
            }
        }
    }

    @Test
    fun constrainsInvalidValuesAndUsesTheSameRelativeFrameForDifferentPreviewSizes() {
        assertEquals(PhotoCrop(1f, 1f, -1f), PhotoCrop(-1f, 5f, -5f).constrained())
        assertEquals(
            PhotoCrop(),
            PhotoCrop(Float.NaN, Float.NaN, Float.POSITIVE_INFINITY).constrained(),
        )
        val crop = PhotoCrop(2f, 0.5f, -0.5f)
        val small = crop.placement(900f, 400f, 350f, 450f)
        val large = crop.placement(900f, 400f, 700f, 900f)
        assertEquals(small.width * 2, large.width)
        assertEquals(small.height * 2, large.height)
        assertEquals(small.left * 2, large.left)
        assertEquals(small.top * 2, large.top)
    }
}
