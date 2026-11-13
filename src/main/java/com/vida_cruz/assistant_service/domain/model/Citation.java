package com.vida_cruz.assistant_service.domain.model;

import com.vida_cruz.assistant_service.domain.util.Texts;

/** Reference to a section of a protocol: what an answer cites as its source. */
public record Citation(String protocol, String section) {

    public Citation {
        protocol = Texts.requireNotBlank(protocol, "protocol");
        section = Texts.requireNotBlank(section, "section");
    }
}