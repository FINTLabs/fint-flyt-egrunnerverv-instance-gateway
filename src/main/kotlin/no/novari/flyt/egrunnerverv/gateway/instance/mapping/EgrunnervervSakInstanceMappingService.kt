package no.novari.flyt.egrunnerverv.gateway.instance.mapping

import no.novari.flyt.egrunnerverv.gateway.exception.ArchiveResourceNotFoundException
import no.novari.flyt.egrunnerverv.gateway.exception.NonMatchingEmailDomainWithOrgIdException
import no.novari.flyt.egrunnerverv.gateway.infrastructure.slack.SlackAlertService
import no.novari.flyt.egrunnerverv.gateway.instance.ResourceRepository
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervSakInstance
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervSakKlassering
import no.novari.flyt.egrunnerverv.gateway.instance.model.EgrunnervervSaksPart
import no.novari.flyt.gateway.instance.InstanceMapper
import no.novari.flyt.gateway.instance.model.File
import no.novari.flyt.gateway.instance.model.instance.InstanceObject
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EgrunnervervSakInstanceMappingService(
    private val resourceRepository: ResourceRepository,
    private val formattingUtilsService: FormattingUtilsService,
    private val slackAlertService: SlackAlertService,
    @param:Value("\${novari.flyt.egrunnerverv.checkSaksansvarligEpost:true}")
    private val checkSaksansvarligEpost: Boolean,
    @param:Value("\${novari.flyt.egrunnerverv.checkEmailDomain:true}")
    private val checkEmailDomain: Boolean,
    @param:Value("\${fint.org-id}")
    private val orgId: String,
) : InstanceMapper<EgrunnervervSakInstance> {
    override fun map(
        sourceApplicationId: Long,
        incomingInstance: EgrunnervervSakInstance,
        persistFile: (File) -> UUID,
    ): InstanceObject {
        val saksansvarligEpostFormatted = formattingUtilsService.formatEmail(incomingInstance.saksansvarligEpost)

        if (checkEmailDomain) {
            val domain =
                formattingUtilsService.extractEmailDomain(saksansvarligEpostFormatted)
                    ?: throw IllegalArgumentException("Invalid email address")
            if (domain != orgId) {
                throw NonMatchingEmailDomainWithOrgIdException(domain, orgId)
            }
        }

        val saksansvarlig =
            if (checkSaksansvarligEpost) {
                resourceRepository.getArkivressursHrefFromPersonEmail(saksansvarligEpostFormatted)
                    ?: throw ArchiveResourceNotFoundException(saksansvarligEpostFormatted, slackAlertService)
            } else {
                ""
            }

        return InstanceObject(
            valuePerKey =
                linkedMapOf(
                    "sys_id" to incomingInstance.sysId,
                    "knr" to incomingInstance.knr,
                    "gnr" to incomingInstance.gnr,
                    "bnr" to incomingInstance.bnr,
                    "fnr" to incomingInstance.fnr,
                    "snr" to incomingInstance.snr,
                    "takstnummer" to incomingInstance.takstnummer,
                    "tittel" to incomingInstance.tittel,
                    "saksansvarligEpost" to saksansvarligEpostFormatted,
                    "saksansvarlig" to saksansvarlig,
                    "eierforholdsnavn" to incomingInstance.eierforholdsnavn,
                    "eierforholdskode" to incomingInstance.eierforholdskode,
                    "prosjektnr" to incomingInstance.prosjektnr,
                    "prosjektnavn" to incomingInstance.prosjektnavn,
                    "kommunenavn" to (formattingUtilsService.formatKommunenavn(incomingInstance.kommunenavn) ?: ""),
                    "adresse" to incomingInstance.adresse,
                ),
            objectCollectionPerKey =
                mutableMapOf(
                    "saksparter" to incomingInstance.saksparter.map(::toInstanceObject),
                    "klasseringer" to incomingInstance.klasseringer.map(::toInstanceObject),
                ),
        )
    }

    private fun toInstanceObject(saksPart: EgrunnervervSaksPart): InstanceObject =
        InstanceObject(
            valuePerKey =
                mapOf(
                    "navn" to saksPart.navn,
                    "organisasjonsnummer" to saksPart.organisasjonsnummer,
                    "epost" to saksPart.epost,
                    "telefon" to saksPart.telefon,
                    "postadresse" to saksPart.postadresse,
                    "postnummer" to saksPart.postnummer,
                    "poststed" to saksPart.poststed,
                ),
        )

    private fun toInstanceObject(klassering: EgrunnervervSakKlassering): InstanceObject =
        InstanceObject(
            valuePerKey =
                mapOf(
                    "ordningsprinsipp" to klassering.ordningsprinsipp,
                    "ordningsverdi" to klassering.ordningsverdi,
                    "beskrivelse" to klassering.beskrivelse,
                    "sortering" to klassering.sortering,
                    "untattOffentlighet" to klassering.untattOffentlighet,
                ),
        )
}
