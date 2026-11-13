package com.vida_cruz.assistant_service.infraestructure.entrypoints.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/** Subscribes to a reactive stream when the application starts and cancels it when it stops. */
@Slf4j
public class StreamRunner implements SmartLifecycle {

    private final String name;
    private final Supplier<? extends Flux<?>> stream;
    private Disposable subscription; // the only mutable state: the lifecycle

    public StreamRunner(String name, Supplier<? extends Flux<?>> stream) {
        this.name = Objects.requireNonNull(name);
        this.stream = Objects.requireNonNull(stream);
    }

    @Override
    public synchronized void start() {
        subscription = isRunning() ? subscription : launch();
    }

    @Override
    public synchronized void stop() {
        Optional.ofNullable(subscription).ifPresent(Disposable::dispose);
        subscription = null;
    }

    @Override
    public synchronized boolean isRunning() {
        return Optional.ofNullable(subscription)
                .map(running -> !running.isDisposed())
                .orElse(false);
    }

    private Disposable launch() {
        log.info("Starting {}", name);
        return stream.get().subscribe(
                item -> { },
                error -> log.error("{} terminated unexpectedly", name, error));
    }
}