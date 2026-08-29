package com.resolveiq.analysis.application.service;

import com.resolveiq.analysis.domain.model.AnalysisResult;
import com.resolveiq.analysis.domain.repository.AnalysisResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisPersistenceService {
    private final AnalysisResultRepository repository;
    public AnalysisPersistenceService(AnalysisResultRepository repository) { this.repository = repository; }
    @Transactional
    public AnalysisResult save(AnalysisResult result) { return repository.save(result); }
}
