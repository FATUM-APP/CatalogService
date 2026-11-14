package fatum.catalog.client

import fatum.catalog.client.auth.CognitoTokenProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.LocalDate


@Component
class UserServiceClient(
    private val tokenProvider: CognitoTokenProvider,
    @Value("\${fatum.base-url}") baseUrl: String
) {
    private val restClient = RestClient.builder()
        .baseUrl(baseUrl)
        .build()

    fun getUserById(userId: String): M2User {
        val token = tokenProvider.getAccessToken()

        return restClient.get()
            .uri("/users/by-id/{id}", userId)
            .header("Authorization", "Bearer $token")
            .retrieve()
            .body(M2User::class.java)
            ?: throw IllegalStateException("Respuesta vacía del User Service")
    }
}


data class M2User(
    val userId: String,
    val name: String,
    val username: String,
    val birthDate: LocalDate,
    val isProfessional: Boolean,
    val isVerified: Boolean,
    val isActive: Boolean,
    val gender: String,
    val principalAddress: AddressDTO
){
    val isValid get() = isActive && isVerified
}


data class AddressDTO(
    val residence: String,
    val alias: String,
    val city: String,
    val state: String,
    val country: String,
)