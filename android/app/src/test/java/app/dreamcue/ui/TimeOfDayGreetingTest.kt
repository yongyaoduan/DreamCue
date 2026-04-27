package app.dreamcue.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TimeOfDayGreetingTest {
    @Test
    fun greetingUsesLocalHourBoundaries() {
        assertEquals("Good night", timeOfDayGreeting(calendarAt(4)))
        assertEquals("Good morning", timeOfDayGreeting(calendarAt(5)))
        assertEquals("Good morning", timeOfDayGreeting(calendarAt(11)))
        assertEquals("Good afternoon", timeOfDayGreeting(calendarAt(12)))
        assertEquals("Good evening", timeOfDayGreeting(calendarAt(17)))
        assertEquals("Good night", timeOfDayGreeting(calendarAt(22)))
    }

    private fun calendarAt(hour: Int): Calendar {
        return Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply {
            set(2026, Calendar.APRIL, 26, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
