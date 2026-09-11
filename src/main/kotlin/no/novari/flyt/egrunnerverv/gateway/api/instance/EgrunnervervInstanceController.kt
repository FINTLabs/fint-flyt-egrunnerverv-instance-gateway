package no.novari.flyt.egrunnerverv.gateway.api.instance

import jakarta.validation.Valid
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervJournalpostInstance
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervJournalpostInstanceBody
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervSakInstance
import no.novari.flyt.gateway.instance.InstanceProcessor
import no.novari.flyt.webresourceserver.UrlPaths.EXTERNAL_API
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("$EXTERNAL_API/egrunnerverv/instances/{orgNr}")
class EgrunnervervInstanceController(
    private val sakInstanceProcessor: InstanceProcessor<EgrunnervervSakInstance>,
    private val journalpostInstanceProcessor: InstanceProcessor<EgrunnervervJournalpostInstance>,
) {
    @PostMapping("archive")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun postSakInstance(
        @Valid @RequestBody egrunnervervSakInstance: EgrunnervervSakInstance,
        authentication: Authentication,
    ) {
        sakInstanceProcessor.processInstance(authentication, egrunnervervSakInstance)
    }

    @PostMapping("document")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun postJournalpostInstance(
        @Valid @RequestBody body: EgrunnervervJournalpostInstanceBody,
        @RequestParam("id") saksnummer: String,
        authentication: Authentication,
    ) {
        journalpostInstanceProcessor.processInstance(
            authentication,
            EgrunnervervJournalpostInstance(
                egrunnervervJournalpostInstanceBody = body,
                saksnummer = saksnummer,
            ),
        )
    }
}
