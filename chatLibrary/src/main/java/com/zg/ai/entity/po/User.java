package com.zg.ai.entity.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zg.ai.entity.po.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("users")
public class User extends BaseEntity {
    /**
     * 用户名
     */
    private String username;

    /**
     * 角色
     */
    private String role;

    /**
     * 上传权限
     */
    private Boolean uploadPermission;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 密码哈希
     */
    private String passwordHash;

    /**
     * 总文档数
     */
    private Integer totalDocuments;

    /**
     * 最后登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;
}