package no.novari.flyt.egrunnerverv.gateway.infrastructure.kafka.resources

import no.novari.cache.FintCache
import no.novari.fint.model.resource.FintLinks
import no.novari.fint.model.resource.administrasjon.personal.PersonalressursResource
import no.novari.fint.model.resource.arkiv.kodeverk.JournalStatusResource
import no.novari.fint.model.resource.arkiv.kodeverk.JournalpostTypeResource
import no.novari.fint.model.resource.arkiv.kodeverk.SkjermingshjemmelResource
import no.novari.fint.model.resource.arkiv.kodeverk.TilgangsrestriksjonResource
import no.novari.fint.model.resource.arkiv.noark.AdministrativEnhetResource
import no.novari.fint.model.resource.arkiv.noark.ArkivressursResource
import no.novari.fint.model.resource.felles.PersonResource
import no.novari.flyt.egrunnerverv.gateway.ResourceLinkUtil
import no.novari.kafka.consuming.ErrorHandlerConfiguration
import no.novari.kafka.consuming.ErrorHandlerFactory
import no.novari.kafka.consuming.ListenerConfiguration
import no.novari.kafka.consuming.ParameterizedListenerContainerFactoryService
import no.novari.kafka.topic.name.EntityTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer

@Configuration
class ResourceEntityConsumersConfiguration(
    private val listenerFactoryService: ParameterizedListenerContainerFactoryService,
    private val errorHandlerFactory: ErrorHandlerFactory,
) {
    @Bean
    fun administrativEnhetResourceEntityConsumer(
        administrativEnhetResourceCache: FintCache<String, AdministrativEnhetResource>,
    ): ConcurrentMessageListenerContainer<String, AdministrativEnhetResource> =
        createCacheConsumer(
            resourceName = "arkiv-noark-administrativenhet",
            resourceClass = AdministrativEnhetResource::class.java,
            cache = administrativEnhetResourceCache,
        )

    @Bean
    fun arkivressursResourceEntityConsumer(
        arkivressursResourceCache: FintCache<String, ArkivressursResource>,
    ): ConcurrentMessageListenerContainer<String, ArkivressursResource> =
        createCacheConsumer(
            resourceName = "arkiv-noark-arkivressurs",
            resourceClass = ArkivressursResource::class.java,
            cache = arkivressursResourceCache,
        )

    @Bean
    fun tilgangsrestriksjonResourceEntityConsumer(
        tilgangsrestriksjonResourceCache: FintCache<String, TilgangsrestriksjonResource>,
    ): ConcurrentMessageListenerContainer<String, TilgangsrestriksjonResource> =
        createCacheConsumer(
            resourceName = "arkiv-kodeverk-tilgangsrestriksjon",
            resourceClass = TilgangsrestriksjonResource::class.java,
            cache = tilgangsrestriksjonResourceCache,
        )

    @Bean
    fun skjermingshjemmelResourceEntityConsumer(
        skjermingshjemmelResourceCache: FintCache<String, SkjermingshjemmelResource>,
    ): ConcurrentMessageListenerContainer<String, SkjermingshjemmelResource> =
        createCacheConsumer(
            resourceName = "arkiv-kodeverk-skjermingshjemmel",
            resourceClass = SkjermingshjemmelResource::class.java,
            cache = skjermingshjemmelResourceCache,
        )

    @Bean
    fun journalstatusResourceEntityConsumer(
        journalStatusResourceCache: FintCache<String, JournalStatusResource>,
    ): ConcurrentMessageListenerContainer<String, JournalStatusResource> =
        createCacheConsumer(
            resourceName = "arkiv-kodeverk-journalstatus",
            resourceClass = JournalStatusResource::class.java,
            cache = journalStatusResourceCache,
        )

    @Bean
    fun journalposttypeEntityConsumer(
        journalpostTypeResourceCache: FintCache<String, JournalpostTypeResource>,
    ): ConcurrentMessageListenerContainer<String, JournalpostTypeResource> =
        createCacheConsumer(
            resourceName = "arkiv-kodeverk-journalposttype",
            resourceClass = JournalpostTypeResource::class.java,
            cache = journalpostTypeResourceCache,
        )

    @Bean
    fun personalressursResourceEntityConsumer(
        personalressursResourceCache: FintCache<String, PersonalressursResource>,
    ): ConcurrentMessageListenerContainer<String, PersonalressursResource> =
        createCacheConsumer(
            resourceName = "administrasjon-personal-personalressurs",
            resourceClass = PersonalressursResource::class.java,
            cache = personalressursResourceCache,
        )

    @Bean
    fun personResourceEntityConsumer(
        personResourceCache: FintCache<String, PersonResource>,
    ): ConcurrentMessageListenerContainer<String, PersonResource> =
        createCacheConsumer(
            resourceName = "administrasjon-personal-person",
            resourceClass = PersonResource::class.java,
            cache = personResourceCache,
        )

    private fun <T : FintLinks> createCacheConsumer(
        resourceName: String,
        resourceClass: Class<T>,
        cache: FintCache<String, T>,
    ): ConcurrentMessageListenerContainer<String, T> {
        val listenerConfig =
            ListenerConfiguration
                .stepBuilder()
                .groupIdApplicationDefault()
                .maxPollRecordsKafkaDefault()
                .maxPollIntervalKafkaDefault()
                .seekToBeginningOnAssignment()
                .build()

        val errorHandlerConfiguration: ErrorHandlerConfiguration<T> =
            ErrorHandlerConfiguration
                .stepBuilder<T>()
                .noRetries()
                .skipFailedRecords()
                .build()

        return listenerFactoryService
            .createRecordListenerContainerFactory(
                resourceClass,
                { record ->
                    val value = record.value()
                    cache.put(ResourceLinkUtil.getSelfLinks(value), value)
                },
                listenerConfig,
                errorHandlerFactory.createErrorHandler(
                    errorHandlerConfiguration,
                ),
            ).createContainer(
                EntityTopicNameParameters
                    .builder()
                    .topicNamePrefixParameters(
                        TopicNamePrefixParameters
                            .stepBuilder()
                            .orgIdApplicationDefault()
                            .domainContextApplicationDefault()
                            .build(),
                    ).resourceName(resourceName)
                    .build(),
            )
    }
}
