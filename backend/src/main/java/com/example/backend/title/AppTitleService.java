package com.example.backend.title;

import org.springframework.stereotype.Service;

@Service
public class AppTitleService {

    private static final long TITLE_ID = 1L;

    private final AppTitleRepository appTitleRepository;

    public AppTitleService(AppTitleRepository appTitleRepository) {
        this.appTitleRepository = appTitleRepository;
    }

    public AppTitleDTO getAppTitle() {
        var appTitleOptional = appTitleRepository.findById(TITLE_ID);
        return appTitleOptional
                .map(entity -> new AppTitleDTO(entity.getId(), entity.getText()))
                .orElseThrow(() -> new IllegalStateException(
                        "App title row with id " + TITLE_ID + " is missing in the database"));
    }
}
