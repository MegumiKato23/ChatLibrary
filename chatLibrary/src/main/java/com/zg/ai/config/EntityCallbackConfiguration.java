package com.zg.ai.config;

import com.zg.ai.entity.po.base.BaseEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.mapping.event.AfterConvertCallback;
import org.springframework.data.r2dbc.mapping.event.AfterSaveCallback;
import reactor.core.publisher.Mono;

@Configuration
public class EntityCallbackConfiguration {

    @Bean
    public AfterConvertCallback<BaseEntity> afterConvertCallback() {
        return (entity, table) -> {
            entity.setNew(false);
            return Mono.just(entity);
        };
    }

    @Bean
    public AfterSaveCallback<BaseEntity> afterSaveCallback() {
        return (entity, outbox, table) -> {
            entity.setNew(false);
            return Mono.just(entity);
        };
    }
}
