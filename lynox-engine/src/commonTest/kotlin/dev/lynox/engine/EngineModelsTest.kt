package dev.lynox.engine

import dev.lynox.engine.model.MediaItem
import dev.lynox.engine.model.SearchRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EngineModelsTest {
    @Test
    fun searchRequestKeepsQueryAndPageSize() {
        val request = SearchRequest(query = "test", pageSize = 10)
        assertEquals("test", request.query)
        assertEquals(10, request.pageSize)
    }

    @Test
    fun searchRequestRejectsInvalidPageSize() {
        assertFailsWith<IllegalArgumentException> {
            SearchRequest(query = "test", pageSize = 100)
        }
    }

    @Test
    fun mediaItemStoresProviderNeutralMetadata() {
        val item = MediaItem(id = "abc", title = "Example")
        assertEquals("abc", item.id)
        assertEquals("Example", item.title)
    }
}
