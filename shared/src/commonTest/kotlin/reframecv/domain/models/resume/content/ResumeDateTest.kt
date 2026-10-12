package reframecv.domain.models.resume.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResumeDateTest {
    @Test
    fun supportsPartialDatesAndIncompletePeriods() {
        assertNull(ResumeDate(2020).month)
        assertEquals(12, ResumeDate(2020, 12).month)
        assertEquals(ResumeDateRange(), ResumeExperience().period)
        assertTrue(ResumeDateRange(start = ResumeDate(2020), isCurrent = true).isCurrent)
    }

    @Test
    fun rejectsInvalidDatesAndContradictoryCurrentPeriods() {
        listOf(0, 10000).forEach { year ->
            assertFailsWith<IllegalArgumentException> { ResumeDate(year) }
        }
        listOf(0, 13).forEach { month ->
            assertFailsWith<IllegalArgumentException> { ResumeDate(2020, month) }
        }
        assertFailsWith<IllegalArgumentException> {
            ResumeDateRange(end = ResumeDate(2020), isCurrent = true)
        }
    }
}
