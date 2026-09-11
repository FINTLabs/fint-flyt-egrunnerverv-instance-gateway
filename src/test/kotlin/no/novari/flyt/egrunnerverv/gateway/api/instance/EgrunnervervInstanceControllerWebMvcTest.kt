package no.novari.flyt.egrunnerverv.gateway.api.instance

import com.fasterxml.jackson.databind.ObjectMapper
import no.novari.flyt.egrunnerverv.gateway.api.error.EgrunnervervGlobalExceptionHandler
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervJournalpostDocument
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervJournalpostInstanceBody
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervJournalpostReceiver
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervSakInstance
import no.novari.flyt.gateway.instance.InstanceProcessor
import no.novari.flyt.webresourceserver.UrlPaths.EXTERNAL_API
import no.novari.flyt.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorityMappingService
import no.novari.flyt.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorizationRequestService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [EgrunnervervInstanceController::class])
@Import(EgrunnervervGlobalExceptionHandler::class)
class EgrunnervervInstanceControllerWebMvcTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean(name = "sakInstanceProcessor")
    private lateinit var sakInstanceProcessor: InstanceProcessor<EgrunnervervSakInstance>

    @MockitoBean(name = "journalpostInstanceProcessor")
    private lateinit var journalpostInstanceProcessor: InstanceProcessor<*>

    @MockitoBean
    private lateinit var sourceApplicationAuthorizationRequestService: SourceApplicationAuthorizationRequestService

    @MockitoBean
    private lateinit var sourceApplicationAuthorityMappingService: SourceApplicationAuthorityMappingService

    @Test
    fun `POST archive returns 202 and processes instance`() {
        mockMvc
            .perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post("$EXTERNAL_API/egrunnerverv/instances/999/archive")
                    .with(user("test-user"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(validSakInstance())),
            ).andExpect(status().isAccepted)

        verify(sakInstanceProcessor).processInstance(any<Authentication>(), any<EgrunnervervSakInstance>())
    }

    @Test
    fun `POST archive with invalid body returns ErrorResponse 400`() {
        mockMvc
            .perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post("$EXTERNAL_API/egrunnerverv/instances/999/archive")
                    .with(user("test-user"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.path").value("$EXTERNAL_API/egrunnerverv/instances/999/archive"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `POST archive when processor throws IllegalArgumentException returns ErrorResponse 400`() {
        doThrow(IllegalArgumentException("bad input"))
            .whenever(sakInstanceProcessor)
            .processInstance(any<Authentication>(), any<EgrunnervervSakInstance>())

        mockMvc
            .perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post("$EXTERNAL_API/egrunnerverv/instances/999/archive")
                    .with(user("test-user"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(validSakInstance())),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Invalid request"))
    }

    @Test
    fun `POST archive when processor throws unexpected exception returns ErrorResponse 500`() {
        doThrow(RuntimeException("boom"))
            .whenever(sakInstanceProcessor)
            .processInstance(any<Authentication>(), any<EgrunnervervSakInstance>())

        mockMvc
            .perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post("$EXTERNAL_API/egrunnerverv/instances/999/archive")
                    .with(user("test-user"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(validSakInstance())),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.error").value("Internal Server Error"))
    }

    @Test
    fun `POST document without id returns ErrorResponse 500`() {
        mockMvc
            .perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post("$EXTERNAL_API/egrunnerverv/instances/999/document")
                    .with(user("test-user"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(validJournalpostBody())),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.error").value("Internal Server Error"))
    }

    private fun validSakInstance(): EgrunnervervSakInstance =
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
            saksansvarligEpost = "test@novari.no",
            eierforholdsnavn = "Eiernavn",
            eierforholdskode = "KODE",
            prosjektnr = "42",
            prosjektnavn = "Prosjekt",
            kommunenavn = "OSLO",
            adresse = "Gate 1",
        )

    private fun validJournalpostBody(): EgrunnervervJournalpostInstanceBody =
        EgrunnervervJournalpostInstanceBody(
            sysId = "SYS123",
            table = "table",
            tittel = "Sakstittel",
            dokumentDato = "2024-01-01",
            forsendelsesMate = "Digital",
            kommunenavn = "OSLO",
            knr = "1",
            gnr = "2",
            bnr = "3",
            fnr = "4",
            snr = "5",
            id = "id-1",
            maltittel = "Mal",
            prosjektnavn = "Prosjekt",
            saksbehandlerEpost = "test@novari.no",
            dokumenter =
                listOf(
                    EgrunnervervJournalpostDocument(
                        tittel = "Hoveddok",
                        filnavn = "hoved.pdf",
                        dokumentBase64 = "AAAA",
                        hoveddokument = true,
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
        )
}
