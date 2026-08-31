package com.resolveiq.orchestration.application.service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class ResilientDependencyExecutor {
    @CircuitBreaker(name = "analysis")
    @Retry(name = "analysis")
    @Bulkhead(name = "analysis", type = Bulkhead.Type.SEMAPHORE)
    public <T> T analysis(Supplier<T> operation) { return operation.get(); }

    @CircuitBreaker(name = "routing")
    @Retry(name = "routing")
    @Bulkhead(name = "routing", type = Bulkhead.Type.SEMAPHORE)
    public <T> T routing(Supplier<T> operation) { return operation.get(); }

    @CircuitBreaker(name = "rag")
    @Retry(name = "rag")
    @Bulkhead(name = "rag", type = Bulkhead.Type.SEMAPHORE)
    public <T> T rag(Supplier<T> operation) { return operation.get(); }
}
