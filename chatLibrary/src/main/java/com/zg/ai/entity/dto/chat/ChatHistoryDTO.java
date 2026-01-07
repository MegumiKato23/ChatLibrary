package com.zg.ai.entity.dto.chat;

import com.zg.ai.entity.dto.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.Size;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChatHistoryDTO extends BaseDTO {
    @Size(max = 255, message = "标题长度不能超过255")
    private String title;
}
