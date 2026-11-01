package fatum.catalog.dto

import java.time.LocalDateTime

data class ChangeUnavailabilitySchedule(
    private val _id: String,
    val start: LocalDateTime? = null,
    val end: LocalDateTime? = null
){
    val id = _id.trim()
}
