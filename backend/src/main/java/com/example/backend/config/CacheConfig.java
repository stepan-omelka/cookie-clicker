package com.example.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.WebContentInterceptor;

@Configuration
public class CacheConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        var noStore = new WebContentInterceptor();
        noStore.addCacheMapping(CacheControl.noStore(), "/api/**");
        registry.addInterceptor(noStore);
    }

    // the jar dates every file to 1980, so Last-Modified would keep an old index.html alive after a deploy
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/index.html")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache())
                .setUseLastModified(false);
    }
}
