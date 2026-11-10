package com.example.backend.title;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/title")
public class AppTitleController {
    private final AppTitleService appTitleService;

    public AppTitleController(AppTitleService appTitleService) {
        this.appTitleService = appTitleService;
    }

    @GetMapping
    public AppTitleDTO getTitle() {
        return appTitleService.getAppTitle();
    }
}
