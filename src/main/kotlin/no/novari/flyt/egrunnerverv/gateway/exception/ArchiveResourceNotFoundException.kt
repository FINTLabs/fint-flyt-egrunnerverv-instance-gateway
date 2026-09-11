package no.novari.flyt.egrunnerverv.gateway.exception

import no.novari.flyt.egrunnerverv.gateway.infrastructure.slack.SlackAlertService
import no.novari.flyt.gateway.instance.exception.AbstractInstanceRejectedException

class ArchiveResourceNotFoundException(
    email: String,
    slackAlertService: SlackAlertService,
) : AbstractInstanceRejectedException(formatMessage(email)) {
    init {
        slackAlertService.sendMessage(formatMessage(email))
    }

    companion object {
        private fun formatMessage(email: String): String = "No archive resource found for saksansvarligEpost='$email'"
    }
}
