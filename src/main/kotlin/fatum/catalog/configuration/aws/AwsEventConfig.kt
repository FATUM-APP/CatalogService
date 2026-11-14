package fatum.catalog.configuration.aws

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.eventbridgev2.EventBridgeV2Client

/**
 * Mismo criterio que en UserService: credenciales del rol de la tarea ECS, sin llaves fijas.
 */
@Configuration
class AwsEventConfig(
    @Value("\${aws.region:\${AWS_REGION:us-east-1}}") private val region: String
) {

    @Bean
    fun eventBridgeClient(): EventBridgeV2Client =
        EventBridgeV2Client.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .build()
}
