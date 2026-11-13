package com.vida_cruz.assistant_service.domain.model;

import com.vida_cruz.assistant_service.domain.util.Texts;

/** A fragment of the corpus: one section of one protocol. It is the unit that is retrieved and cited. */
public record ProtocolSection(String protocolId, String protocolTitle, String name, String content) {

    public ProtocolSection {
        protocolId = Texts.requireNotBlank(protocolId, "protocolId");
        protocolTitle = Texts.requireNotBlank(protocolTitle, "protocolTitle");
        name = Texts.requireNotBlank(name, "name");
        content = Texts.requireNotBlank(content, "content");
    }

    public Citation citation() {
        return new Citation(protocolId, name);
    }
}