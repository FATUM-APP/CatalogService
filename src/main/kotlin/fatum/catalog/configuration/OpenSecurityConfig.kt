package fatum.catalog.configuration

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * Solo para desarrollo local: `SECURITY_ENABLED=false` deja todos los endpoints abiertos.
 * En cualquier otro entorno la seguridad debe estar encendida.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = ["app.security.enabled"], havingValue = "false")
class OpenSecurityConfig {

    @Bean
    fun openSecurityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }
        .cors { }
        .authorizeHttpRequests { it.anyRequest().permitAll() }
        .build()
}
