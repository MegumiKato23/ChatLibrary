package com.zg.ai.entity.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户更新信息请求
 */
@Data
public class UserUpdateDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 24, message = "用户名长度必须在3-24之间")
    @Pattern(regexp = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$",
            message = "用户名只能包含字母、数字、下划线和中文字符")
    private String username;

    private String email;

//    private boolean uploadPermission;
}
