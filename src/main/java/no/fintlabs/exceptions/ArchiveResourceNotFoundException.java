package no.fintlabs.exceptions;

import no.fintlabs.gateway.instance.AbstractInstanceRejectedException;

public class ArchiveResourceNotFoundException extends AbstractInstanceRejectedException {
    public ArchiveResourceNotFoundException(String email) {
        super("No archive resource found for saksansvarligEpost='" + email + "'");
    }
}
