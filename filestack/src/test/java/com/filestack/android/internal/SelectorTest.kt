package com.filestack.android.internal

import com.filestack.android.Selection
import org.junit.Assert
import org.junit.Test

import org.junit.Assert.*

class SingleSelectorTest {

    val selector = Selector.Single(SimpleSelectionSaver())

    @Test
    fun `toggles only one item`() {
        val selections = listOf(
                selection("1"),
                selection("2"),
                selection("3"),
                selection("4")
        )

        selector.toggle(selections[0])
        selector.toggle(selections[1])
        selector.toggle(selections[2])
        selector.toggle(selections[3])

        assertTrue(selector.isSelected(selections[0]))
        assertFalse(selector.isSelected(selections[1]))
        assertFalse(selector.isSelected(selections[2]))
        assertFalse(selector.isSelected(selections[3]))

        selector.toggle(selections[0])
        assertFalse(selector.isSelected(selections[0]))

        selector.toggle(selections[1])
        assertTrue(selector.isSelected(selections[1]))
    }
}

class MultiSelectorTest {

    val selector = Selector.Multi(SimpleSelectionSaver())

    @Test
    fun `can toggle multiple items`() {
        val selections = listOf(
                selection("1"),
                selection("2"),
                selection("3"),
                selection("4")
        )

        selector.toggle(selections[0])
        selector.toggle(selections[1])
        selector.toggle(selections[2])
        selector.toggle(selections[3])

        assertTrue(selector.isSelected(selections[0]))
        assertTrue(selector.isSelected(selections[1]))
        assertTrue(selector.isSelected(selections[2]))
        assertTrue(selector.isSelected(selections[3]))

        selector.toggle(selections[0])
        assertFalse(selector.isSelected(selections[0]))

        selector.toggle(selections[1])
        assertFalse(selector.isSelected(selections[1]))

        selector.toggle(selections[0])
        assertTrue(selector.isSelected(selections[0]))
    }
}

private fun selection(id: String): Selection {
    return Selection("provider", "path", "mimeType", id)
}
