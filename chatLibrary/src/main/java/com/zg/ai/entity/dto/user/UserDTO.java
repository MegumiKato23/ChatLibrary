package com.zg.ai.entity.dto.user;

import com.zg.ai.entity.dto.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserDTO extends BaseDTO {
    private String username;
    private String email;
    private String role;
    private Integer totalDocuments;
    private Boolean uploadPermission;
    private LocalDateTime lastLoginAt;
}
