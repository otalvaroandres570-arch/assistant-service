package com.vida_cruz.assistant_service.infraestructure.adapters.corpus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vida_cruz.assistant_service.domain.model.ProtocolCorpus;
import com.vida_cruz.assistant_service.domain.model.ProtocolSection;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/** Reads the protocols from a JSON file in the classpath. Runs once at startup, never in a request. */
@RequiredArgsConstructor
public class ClasspathProtocolCorpusLoader {

    private final ObjectMapper objectMapper;

    public ProtocolCorpus load(String location) {
        return toCorpus(read(location));
    }

    private List<ProtocolFile> read(String location) {
        try (InputStream input = new ClassPathResource(location).getInputStream()) {
            return objectMapper.readValue(input, new TypeReference<List<ProtocolFile>>() { });
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the protocol corpus from classpath:" + location, e);
        }
    }

    private static ProtocolCorpus toCorpus(List<ProtocolFile> files) {
        return new ProtocolCorpus(files.stream()
                .flatMap(file -> file.sections().stream()
                        .map(section -> new ProtocolSection(file.id(), file.title(), section.name(), section.content())))
                .toList());
    }

    record ProtocolFile(String id, String title, List<SectionFile> sections) {
    }

    record SectionFile(String name, String content) {
    }
}