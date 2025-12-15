package com.zg.ai.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FeedbackType {
    POSITIVE(0, "积极"),
    NEGATIVE(1, "消极"),
    NEUTRAL(2, "中性");

    @EnumValue
    private final Integer code;

    @JsonValue
    private final String desc;
}
