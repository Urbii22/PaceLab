package com.urbii.pacelab.domain.model

data class HeartRateZone(val number: Int, val lowerBpm: Int, val upperBpm: Int, val name: String = "Zone $number") {
    fun contains(bpm: Int): Boolean = bpm >= lowerBpm && bpm <= upperBpm
}
