package com.zg.ai.entity.dto.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseDTO {
    private String id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
