package no.novari.flyt.egrunnerverv.gateway.exception

import no.novari.flyt.gateway.instance.exception.AbstractInstanceRejectedException

class NonMatchingEmailDomainWithOrgIdException(
    domain: String,
    orgId: String,
) : AbstractInstanceRejectedException("Email domain='$domain' does not match orgId='$orgId'")
