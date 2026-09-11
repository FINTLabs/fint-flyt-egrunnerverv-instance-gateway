package no.novari.flyt.egrunnerverv.gateway.instance.configuration

import no.novari.flyt.egrunnerverv.gateway.instance.mapping.EgrunnervervJournalpostInstanceMappingService
import no.novari.flyt.egrunnerverv.gateway.instance.mapping.EgrunnervervSakInstanceMappingService
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervJournalpostInstance
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervSakInstance
import no.novari.flyt.gateway.instance.InstanceProcessor
import no.novari.flyt.gateway.instance.InstanceProcessorFactoryService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InstanceProcessorConfiguration {
    @Bean
    fun sakInstanceProcessor(
        instanceProcessorFactoryService: InstanceProcessorFactoryService,
        egrunnervervSakInstanceMappingService: EgrunnervervSakInstanceMappingService,
    ): InstanceProcessor<EgrunnervervSakInstance> =
        instanceProcessorFactoryService.createInstanceProcessor(
            sourceApplicationIntegrationId = "sak",
            sourceApplicationInstanceIdFunction = { it.sysId },
            instanceMapper = egrunnervervSakInstanceMappingService,
        )

    @Bean
    fun journalpostInstanceProcessor(
        instanceProcessorFactoryService: InstanceProcessorFactoryService,
        egrunnervervJournalpostInstanceMappingService: EgrunnervervJournalpostInstanceMappingService,
    ): InstanceProcessor<EgrunnervervJournalpostInstance> =
        instanceProcessorFactoryService.createInstanceProcessor(
            sourceApplicationIntegrationId = "journalpost",
            sourceApplicationInstanceIdFunction = { it.egrunnervervJournalpostInstanceBody.sysId },
            instanceMapper = egrunnervervJournalpostInstanceMappingService,
        )
}
