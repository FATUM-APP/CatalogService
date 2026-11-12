package fatum.catalog.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Mismo criterio que en UserService: los orígenes se leen de `app.cors.allowed-origins`
 * (separados por coma). La misma propiedad se reutiliza para el handshake del WebSocket,
 * porque el navegador aplica CORS también al `wss://`.
 *
 * Con Spring Security activo, la fuente que manda es [corsConfigurationSource].
 * Ojo: CORS no es autenticación, solo lo respeta un navegador.
 */
@Configuration
class CorsConfig(
    @Value("\${app.cors.allowed-origins:http://localhost:5173}") private val allowedOrigins: String
) {

    private fun origins(): List<String> =
        allowedOrigins.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    @Bean
    fun corsConfigurer(): WebMvcConfigurer = object : WebMvcConfigurer {
        override fun addCorsMappings(registry: CorsRegistry) {
            registry.addMapping("/**")
                .allowedOriginPatterns(*origins().toTypedArray())
                .allowedMethods(*METHODS.toTypedArray())
                .allowedHeaders("*")
                .allowCredentials(false)
        }
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource =
        UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", CorsConfiguration().apply {
                allowedOriginPatterns = origins()
                allowedMethods = METHODS
                allowedHeaders = listOf("*")
                allowCredentials = false
            })
        }

    private companion object {
        val METHODS = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
    }
}
