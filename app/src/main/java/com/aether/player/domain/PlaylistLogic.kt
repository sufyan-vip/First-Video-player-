package com.aether.player.domain

data class QueueSnapshot(
    val items: List<String>,
    val index: Int,
    val shuffle: Boolean,
    val repeatMode: RepeatMode,
)

enum class RepeatMode { OFF, ONE, ALL }

object PlaylistLogic {
    fun nextIndex(
        size: Int,
        current: Int,
        shuffle: Boolean,
        repeatMode: RepeatMode,
        userNext: Boolean = true,
    ): Int? {
        if (size <= 0) return null
        val idx = current.coerceIn(0, size - 1)
        if (repeatMode == RepeatMode.ONE && !userNext) return idx
        if (shuffle && size > 1) {
            var next = (idx + 1 + (System.nanoTime() % (size - 1)).toInt()) % size
            if (next == idx) next = (idx + 1) % size
            return next
        }
        val next = idx + 1
        return when {
            next < size -> next
            repeatMode == RepeatMode.ALL -> 0
            else -> null
        }
    }

    fun previousIndex(size: Int, current: Int, repeatMode: RepeatMode): Int? {
        if (size <= 0) return null
        val idx = current.coerceIn(0, size - 1)
        val prev = idx - 1
        return when {
            prev >= 0 -> prev
            repeatMode == RepeatMode.ALL -> size - 1
            else -> idx
        }
    }

    fun move(items: List<String>, from: Int, to: Int): List<String> {
        if (from !in items.indices || to !in items.indices || from == to) return items
        val mutable = items.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        return mutable
    }

    fun playNext(items: List<String>, currentIndex: Int, newId: String): Pair<List<String>, Int> {
        if (items.isEmpty()) return listOf(newId) to 0
        val idx = currentIndex.coerceIn(0, items.lastIndex)
        val mutable = items.toMutableList()
        mutable.add(idx + 1, newId)
        return mutable to idx
    }

    fun addToEnd(items: List<String>, id: String): List<String> = items + id

    fun removeAt(items: List<String>, index: Int, currentIndex: Int): Pair<List<String>, Int> {
        if (index !in items.indices) return items to currentIndex
        val mutable = items.toMutableList()
        mutable.removeAt(index)
        val newIndex = when {
            mutable.isEmpty() -> 0
            index < currentIndex -> (currentIndex - 1).coerceAtLeast(0)
            index == currentIndex -> currentIndex.coerceAtMost(mutable.lastIndex)
            else -> currentIndex.coerceAtMost(mutable.lastIndex)
        }
        return mutable to newIndex
    }
}
