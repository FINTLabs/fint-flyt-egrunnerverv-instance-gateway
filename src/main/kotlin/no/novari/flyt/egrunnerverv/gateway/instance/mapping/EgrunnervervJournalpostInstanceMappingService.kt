package no.novari.flyt.egrunnerverv.gateway.instance.mapping

import no.novari.flyt.egrunnerverv.gateway.exception.ArchiveResourceNotFoundException
import no.novari.flyt.egrunnerverv.gateway.exception.NonMatchingEmailDomainWithOrgIdException
import no.novari.flyt.egrunnerverv.gateway.infrastructure.slack.SlackAlertService
import no.novari.flyt.egrunnerverv.gateway.instance.ResourceRepository
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervJournalpostDocument
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervJournalpostInstance
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervJournalpostReceiver
import no.novari.flyt.gateway.instance.InstanceMapper
import no.novari.flyt.gateway.instance.model.File
import no.novari.flyt.gateway.instance.model.instance.InstanceObject
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.MediaTypeFactory
import org.springframework.stereotype.Service
import java.util.Base64
import java.util.UUID

@Service
class EgrunnervervJournalpostInstanceMappingService(
    private val resourceRepository: ResourceRepository,
    private val formattingUtilsService: FormattingUtilsService,
    private val slackAlertService: SlackAlertService,
    @param:Value("\${novari.flyt.egrunnerverv.checkSaksbehandler:true}")
    private val checkSaksbehandler: Boolean,
    @param:Value("\${novari.flyt.egrunnerverv.checkEmailDomain:true}")
    private val checkEmailDomain: Boolean,
    @param:Value("\${fint.org-id}")
    private val orgId: String,
) : InstanceMapper<EgrunnervervJournalpostInstance> {
    override fun map(
        sourceApplicationId: Long,
        incomingInstance: EgrunnervervJournalpostInstance,
        persistFile: (File) -> UUID,
    ): InstanceObject {
        val body = incomingInstance.egrunnervervJournalpostInstanceBody
        val saksbehandlerEpostFormatted = formattingUtilsService.formatEmail(body.saksbehandlerEpost)

        if (checkEmailDomain) {
            val domain =
                formattingUtilsService.extractEmailDomain(saksbehandlerEpostFormatted)
                    ?: throw IllegalArgumentException("Invalid email address")
            if (domain != orgId) {
                throw NonMatchingEmailDomainWithOrgIdException(domain, orgId)
            }
        }

        val saksbehandler =
            if (checkSaksbehandler) {
                resourceRepository.getArkivressursHrefFromPersonEmail(saksbehandlerEpostFormatted)
                    ?: throw ArchiveResourceNotFoundException(saksbehandlerEpostFormatted, slackAlertService)
            } else {
                ""
            }

        val hoveddokument =
            body.dokumenter.firstOrNull { it.hoveddokument }
                ?: throw IllegalStateException("No hoveddokument")
        val vedlegg = body.dokumenter.filterNot { it.hoveddokument }

        val hoveddokumentMediaType = getMediaType(hoveddokument)
        val hoveddokumentFilId =
            persistFile(
                toFile(
                    sourceApplicationId = sourceApplicationId,
                    sourceApplicationInstanceId = body.sysId,
                    document = hoveddokument,
                    type = hoveddokumentMediaType,
                ),
            )

        val vedleggInstanceObjects =
            vedlegg.map { document ->
                val mediaType = getMediaType(document)
                val fileId =
                    persistFile(
                        toFile(
                            sourceApplicationId = sourceApplicationId,
                            sourceApplicationInstanceId = body.sysId,
                            document = document,
                            type = mediaType,
                        ),
                    )
                mapAttachmentDocumentAndFileIdToInstanceObject(document, mediaType, fileId)
            }

        return InstanceObject(
            valuePerKey =
                linkedMapOf(
                    "saksnummer" to incomingInstance.saksnummer,
                    "tittel" to body.tittel,
                    "dokumentNavn" to body.dokumentNavn.orEmpty(),
                    "dokumentDato" to body.dokumentDato,
                    "forsendelsesmaate" to body.forsendelsesMate,
                    "kommunenavn" to (formattingUtilsService.formatKommunenavn(body.kommunenavn) ?: ""),
                    "knr" to body.knr,
                    "gnr" to body.gnr,
                    "bnr" to body.bnr,
                    "fnr" to body.fnr,
                    "snr" to body.snr,
                    "eierforhold" to body.eierforhold.orEmpty(),
                    "id" to body.id,
                    "maltittel" to body.maltittel,
                    "prosjektnavn" to body.prosjektnavn,
                    "saksbehandlerEpost" to saksbehandlerEpostFormatted,
                    "saksbehandler" to saksbehandler,
                    "hoveddokumentTittel" to hoveddokument.tittel,
                    "hoveddokumentFilnavn" to hoveddokument.filnavn,
                    "hoveddokumentMediatype" to hoveddokumentMediaType.toString(),
                    "hoveddokumentFil" to hoveddokumentFilId.toString(),
                ),
            objectCollectionPerKey =
                mutableMapOf(
                    "mottakere" to body.mottakere.map(::toInstanceObject),
                    "vedlegg" to vedleggInstanceObjects,
                ),
        )
    }

    private fun toInstanceObject(receiver: EgrunnervervJournalpostReceiver): InstanceObject =
        InstanceObject(
            valuePerKey =
                mapOf(
                    "navn" to receiver.navn,
                    "organisasjonsnummer" to receiver.organisasjonsnummer.takeIf(::isOrganisasjonsnummer).orEmpty(),
                    "fodselsnummer" to receiver.organisasjonsnummer.takeIf(::isFodselsnummer).orEmpty(),
                    "epost" to receiver.epost,
                    "telefon" to receiver.telefon,
                    "postadresse" to receiver.postadresse,
                    "postnummer" to receiver.postnummer,
                    "poststed" to receiver.poststed,
                ),
        )

    private fun isFodselsnummer(number: String): Boolean = number.length == 11

    private fun isOrganisasjonsnummer(number: String): Boolean = !isFodselsnummer(number)

    private fun getMediaType(document: EgrunnervervJournalpostDocument): MediaType =
        MediaTypeFactory
            .getMediaType(document.filnavn)
            .orElseThrow { IllegalArgumentException("No media type found for fileName=${document.filnavn}") }

    private fun mapAttachmentDocumentAndFileIdToInstanceObject(
        document: EgrunnervervJournalpostDocument,
        mediaType: MediaType,
        fileId: UUID,
    ): InstanceObject =
        InstanceObject(
            valuePerKey =
                mapOf(
                    "tittel" to document.tittel,
                    "filnavn" to document.filnavn,
                    "mediatype" to mediaType.toString(),
                    "fil" to fileId.toString(),
                ),
        )

    private fun toFile(
        sourceApplicationId: Long,
        sourceApplicationInstanceId: String,
        document: EgrunnervervJournalpostDocument,
        type: MediaType,
    ): File =
        File(
            name = document.filnavn,
            sourceApplicationId = sourceApplicationId,
            sourceApplicationInstanceId = sourceApplicationInstanceId,
            type = type,
            encoding = "UTF-8",
            base64Contents = document.dokumentBase64,
        )
}
