package com.whtis.memosly.core.model

data class UserStats(
    val memoDisplayTimestamps: List<String>,
    val memoTypeStats: Map<String, Int>,
    val tagCount: Map<String, Int>,
    /**
     * Total (non-archived) memo count. Older servers only report the display-timestamp
     * list, so the count is derived from its size; v0.31 reports it directly.
     */
    val totalMemoCount: Int,
)
