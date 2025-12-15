package com.zg.ai.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DocumentStatus {
    UPLOADED(0, "已上传"),
    PROCESSING(1, "处理中"),
    PROCESSED(2, "处理完成"),
    FAILED(3, "处理失败"),
    DELETED(4, "已删除");

    @EnumValue
    private final Integer code;

    @JsonValue
    private final String desc;
}
