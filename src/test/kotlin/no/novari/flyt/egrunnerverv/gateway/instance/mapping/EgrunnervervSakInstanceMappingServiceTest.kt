package no.novari.flyt.egrunnerverv.gateway.instance.mapping

import no.novari.flyt.egrunnerverv.gateway.exception.ArchiveResourceNotFoundException
import no.novari.flyt.egrunnerverv.gateway.exception.NonMatchingEmailDomainWithOrgIdException
import no.novari.flyt.egrunnerverv.gateway.infrastructure.slack.SlackAlertService
import no.novari.flyt.egrunnerverv.gateway.instance.ResourceRepository
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervSakInstance
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervSakKlassering
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervSaksPart
import no.novari.flyt.gateway.instance.model.File
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class EgrunnervervSakInstanceMappingServiceTest {
    private val resourceRepository: ResourceRepository = mock()
    private val formattingUtilsService: FormattingUtilsService = mock()
    private val slackAlertService: SlackAlertService = mock()
    private val persistFile: (File) -> UUID = mock()

    private lateinit var service: EgrunnervervSakInstanceMappingService

    @BeforeEach
    fun setUp() {
        service =
            EgrunnervervSakInstanceMappingService(
                resourceRepository = resourceRepository,
                formattingUtilsService = formattingUtilsService,
                slackAlertService = slackAlertService,
                checkSaksansvarligEpost = true,
                checkEmailDomain = true,
                orgId = "novari.no",
            )

        whenever(persistFile(any())).thenReturn(UUID.randomUUID())
        whenever(formattingUtilsService.formatKommunenavn(any())).thenReturn("TestKommunenavn")
        whenever(formattingUtilsService.formatEmail(any())).thenAnswer { it.arguments[0] as String }
        whenever(formattingUtilsService.extractEmailDomain(any())).thenReturn("novari.no")
    }

    @Test
    fun `maps instance correctly when lookup succeeds`() {
        whenever(
            resourceRepository.getArkivressursHrefFromPersonEmail("person@novari.no"),
        ).thenReturn("saksansvarligHref")

        val result = service.map(123L, createInstance(), persistFile)

        assertThat(result.valuePerKey)
            .containsEntry("sys_id", "sys1")
            .containsEntry("saksansvarlig", "saksansvarligHref")
            .containsEntry("kommunenavn", "TestKommunenavn")
        assertThat(result.objectCollectionPerKey["saksparter"]).hasSize(1)
        assertThat(result.objectCollectionPerKey["klasseringer"]).hasSize(1)
    }

    @Test
    fun `throws when email domain does not match orgId`() {
        whenever(formattingUtilsService.extractEmailDomain("person@wrong.no"))
            .thenReturn("wrong.no")

        assertThatThrownBy {
            service.map(
                sourceApplicationId = 123L,
                incomingInstance = createInstance(saksansvarligEpost = "person@wrong.no"),
                persistFile = persistFile,
            )
        }.isInstanceOf(NonMatchingEmailDomainWithOrgIdException::class.java)
    }

    @Test
    fun `throws when archive resource is missing`() {
        whenever(resourceRepository.getArkivressursHrefFromPersonEmail("person@novari.no"))
            .thenReturn(null)

        assertThatThrownBy {
            service.map(sourceApplicationId = 123L, incomingInstance = createInstance(), persistFile = persistFile)
        }.isInstanceOf(ArchiveResourceNotFoundException::class.java)
    }

    private fun createInstance(saksansvarligEpost: String = "person@novari.no"): EgrunnervervSakInstance =
        EgrunnervervSakInstance(
            sysId = "sys1",
            table = "table",
            knr = "1",
            gnr = "2",
            bnr = "3",
            fnr = "4",
            snr = "5",
            takstnummer = "6",
            tittel = "Tittel",
            saksansvarligEpost = saksansvarligEpost,
            eierforholdsnavn = "Eiernavn",
            eierforholdskode = "KODE",
            prosjektnr = "42",
            prosjektnavn = "Prosjekt",
            kommunenavn = "OSLO",
            adresse = "Gate 1",
            saksparter =
                listOf(
                    EgrunnervervSaksPart(
                        navn = "P1",
                        organisasjonsnummer = "999999999",
                        epost = "p1@novari.no",
                        telefon = "11111111",
                        postadresse = "Adr1",
                        postnummer = "1000",
                        poststed = "Oslo",
                    ),
                ),
            klasseringer =
                listOf(
                    EgrunnervervSakKlassering(
                        ordningsprinsipp = "OP",
                        ordningsverdi = "OV",
                        beskrivelse = "Beskrivelse",
                        sortering = "1",
                        untattOffentlighet = "false",
                    ),
                ),
        )
}
