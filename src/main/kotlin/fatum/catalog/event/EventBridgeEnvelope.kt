package fatum.catalog.event

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class EventBridgeEnvelope(
    @JsonProperty("source") val source: String,
    @JsonProperty("detail-type") val detailType: String,
    @JsonProperty("detail") val detail: JsonNode,
    @JsonProperty("id") val id: String? = null,
    @JsonProperty("time") val time: String? = null
)
