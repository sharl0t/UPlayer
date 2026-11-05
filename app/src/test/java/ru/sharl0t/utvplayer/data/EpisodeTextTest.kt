package ru.sharl0t.utvplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeTextTest {
    @Test
    fun number() {
        assertEquals(3, EpisodeText.number("1 сезон 3 серия"))
        assertEquals(12, EpisodeText.number("эксклюзив сезон 12 серия"))
        assertNull(EpisodeText.number("Интервью сезон"))
    }

    @Test
    fun descriptionAndPerson() {
        val d = EpisodeText.description("Мама в 16. 3 серия. Анна, Ростов-на-Дону", "Мама в 16", "1 сезон 3 серия")
        assertEquals("Анна, Ростов-на-Дону", d)
        assertEquals("Анна" to "Ростов-на-Дону", EpisodeText.heroineAndCity(d!!))
    }

    @Test
    fun otherFormatKeepsDescription() {
        val d = EpisodeText.description("Шоу. 5 серия. Большой выпуск про всё на свете", "Шоу", "1 сезон 5 серия")
        assertEquals("Большой выпуск про всё на свете", d)
        assertNull(EpisodeText.heroineAndCity(d!!))
        assertNull(EpisodeText.description("Шоу. 5 серия.", "Шоу", "1 сезон 5 серия"))
        assertNull(EpisodeText.heroineAndCity("Без запятой"))
        assertNull(EpisodeText.heroineAndCity("Он пришёл. Увидел, победил"))
    }
}
