package no.novari.flyt.egrunnerverv.gateway.instance.mapping

import no.novari.flyt.egrunnerverv.gateway.exception.ArchiveResourceNotFoundException
import no.novari.flyt.egrunnerverv.gateway.exception.NonMatchingEmailDomainWithOrgIdException
import no.novari.flyt.egrunnerverv.gateway.infrastructure.slack.SlackAlertService
import no.novari.flyt.egrunnerverv.gateway.instance.ResourceRepository
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervJournalpostDocument
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervJournalpostInstance
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervJournalpostInstanceBody
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervJournalpostReceiver
import no.novari.flyt.gateway.instance.model.File
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class EgrunnervervJournalpostInstanceMappingServiceTest {
    private val resourceRepository: ResourceRepository = mock()
    private val formattingUtilsService: FormattingUtilsService = mock()
    private val slackAlertService: SlackAlertService = mock()
    private val persistFile: (File) -> UUID = mock()

    private lateinit var service: EgrunnervervJournalpostInstanceMappingService

    @BeforeEach
    fun setUp() {
        service =
            EgrunnervervJournalpostInstanceMappingService(
                resourceRepository = resourceRepository,
                formattingUtilsService = formattingUtilsService,
                slackAlertService = slackAlertService,
                checkSaksbehandler = true,
                checkEmailDomain = true,
                orgId = "novari.no",
            )

        whenever(formattingUtilsService.formatEmail(any())).thenAnswer { it.arguments[0] as String }
        whenever(formattingUtilsService.extractEmailDomain(any())).thenReturn("novari.no")
        whenever(formattingUtilsService.formatKommunenavn(any())).thenReturn("FormattedKommune")
        whenever(persistFile(any())).thenReturn(UUID.randomUUID())
    }

    @Test
    fun `maps full instance successfully`() {
        whenever(resourceRepository.getArkivressursHrefFromPersonEmail("person@novari.no"))
            .thenReturn("href123")

        val result = service.map(10L, createInstance(), persistFile)

        assertThat(result.valuePerKey)
            .containsEntry("saksnummer", "SAK123")
            .containsEntry("kommunenavn", "FormattedKommune")
            .containsEntry("saksbehandler", "href123")
        assertThat(result.objectCollectionPerKey["vedlegg"]).hasSize(1)
        assertThat(result.objectCollectionPerKey["mottakere"]).hasSize(1)
        verify(persistFile, times(2)).invoke(any())
    }

    @Test
    fun `throws when email domain is wrong`() {
        whenever(formattingUtilsService.extractEmailDomain("person@wrong.no"))
            .thenReturn("wrong.no")

        assertThatThrownBy {
            service.map(
                sourceApplicationId = 10L,
                incomingInstance = createInstance(saksbehandlerEpost = "person@wrong.no"),
                persistFile = persistFile,
            )
        }.isInstanceOf(NonMatchingEmailDomainWithOrgIdException::class.java)
    }

    @Test
    fun `throws when archive resource is missing`() {
        whenever(resourceRepository.getArkivressursHrefFromPersonEmail(any()))
            .thenReturn(null)

        assertThatThrownBy {
            service.map(sourceApplicationId = 10L, incomingInstance = createInstance(), persistFile = persistFile)
        }.isInstanceOf(ArchiveResourceNotFoundException::class.java)
    }

    private fun createInstance(saksbehandlerEpost: String = "person@novari.no"): EgrunnervervJournalpostInstance =
        EgrunnervervJournalpostInstance(
            saksnummer = "SAK123",
            egrunnervervJournalpostInstanceBody =
                EgrunnervervJournalpostInstanceBody(
                    sysId = "SYS123",
                    table = "table",
                    tittel = "Sakstittel",
                    dokumentNavn = "Doknavn",
                    dokumentDato = "2024-01-01",
                    forsendelsesMate = "Digital",
                    kommunenavn = "OSLO",
                    knr = "1",
                    gnr = "2",
                    bnr = "3",
                    fnr = "4",
                    snr = "5",
                    eierforhold = "Privat",
                    id = "id-1",
                    maltittel = "Mal",
                    prosjektnavn = "Prosjekt",
                    saksbehandlerEpost = saksbehandlerEpost,
                    dokumenter =
                        listOf(
                            EgrunnervervJournalpostDocument(
                                tittel = "Hoveddok",
                                filnavn = "hoved.pdf",
                                dokumentBase64 = "AAAA",
                                hoveddokument = true,
                            ),
                            EgrunnervervJournalpostDocument(
                                tittel = "Vedlegg1",
                                filnavn = "vedlegg1.pdf",
                                dokumentBase64 = "BBBB",
                                hoveddokument = false,
                            ),
                        ),
                    mottakere =
                        listOf(
                            EgrunnervervJournalpostReceiver(
                                navn = "TestNavn",
                                organisasjonsnummer = "123456789",
                                epost = "test@novari.no",
                                telefon = "12345678",
                                postadresse = "Testveien 1",
                                postnummer = "0123",
                                poststed = "Oslo",
                            ),
                        ),
                ),
        )
}
