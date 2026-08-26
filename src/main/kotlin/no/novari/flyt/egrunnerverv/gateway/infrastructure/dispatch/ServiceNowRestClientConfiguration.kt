package no.novari.flyt.egrunnerverv.gateway.infrastructure.dispatch

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
@EnableConfigurationProperties(ServiceNowDispatchProperties::class)
class ServiceNowRestClientConfiguration {
    @Bean("serviceNowAuthorizedClientManager")
    fun serviceNowAuthorizedClientManager(
        clientRegistrationRepository: ClientRegistrationRepository,
        authorizedClientService: OAuth2AuthorizedClientService,
    ): OAuth2AuthorizedClientManager {
        val provider =
            OAuth2AuthorizedClientProviderBuilder
                .builder()
                .clientCredentials()
                .build()

        return AuthorizedClientServiceOAuth2AuthorizedClientManager(
            clientRegistrationRepository,
            authorizedClientService,
        ).apply { setAuthorizedClientProvider(provider) }
    }

    @Bean
    fun restClient(
        props: ServiceNowDispatchProperties,
        @Qualifier("serviceNowAuthorizedClientManager") manager: OAuth2AuthorizedClientManager,
        builder: RestClient.Builder,
    ): RestClient {
        val settings =
            ClientHttpRequestFactorySettings
                .defaults()
                .withConnectTimeout(Duration.ofMinutes(15))
                .withReadTimeout(Duration.ofMinutes(10))

        val requestFactory =
            ClientHttpRequestFactoryBuilder
                .detect()
                .build(settings)

        return builder
            .baseUrl(props.baseUrl)
            .requestFactory(requestFactory)
            .requestInterceptor { request, body, execution ->
                val authorizeRequest =
                    OAuth2AuthorizeRequest
                        .withClientRegistrationId(props.registrationId)
                        .principal("service-now-client")
                        .build()

                val client =
                    manager.authorize(authorizeRequest)
                        ?: error("Authorization against ServiceNow failed")

                request.headers.setBearerAuth(client.accessToken.tokenValue)
                execution.execute(request, body)
            }.build()
    }
}

@ConfigurationProperties(prefix = "novari.flyt.egrunnerverv.dispatch")
data class ServiceNowDispatchProperties(
    var baseUrl: String = "",
    var registrationId: String = "",
)
