package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.Track
import com.example.util.MidnightMixer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LAYA Origin", appName)
    }

    @Test
    fun `test deterministic midnight shuffle is repeatable for same seed`() {
        val list = listOf(
            Track(1, "Track 1", url = "http://a.mp3"),
            Track(2, "Track 2", url = "http://b.mp3"),
            Track(3, "Track 3", url = "http://c.mp3"),
            Track(4, "Track 4", url = "http://d.mp3"),
            Track(5, "Track 5", url = "http://e.mp3")
        )

        val seed = 20261003L
        val shuffle1 = MidnightMixer.shuffleWithSeed(list, seed)
        val shuffle2 = MidnightMixer.shuffleWithSeed(list, seed)

        // Exact same order on any device with same seed
        assertEquals(shuffle1.map { it.id }, shuffle2.map { it.id })

        // Different seed produces different order
        val shuffleDifferentDay = MidnightMixer.shuffleWithSeed(list, 20261004L)
        assertNotEquals(shuffle1.map { it.id }, shuffleDifferentDay.map { it.id })
    }

    @Test
    fun `test seed generation from calendar date`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER) // month is 9 (0-indexed) -> 10
            set(Calendar.DAY_OF_MONTH, 3)
        }
        val seed = MidnightMixer.getTodaySeed(cal)
        assertEquals(20261003L, seed)
    }
}
