package com.zeqi.usermanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改用户名请求
 */
public class RenameRequest {

    @NotBlank(message = "新用户名不能为空")
    @Size(max = 50, message = "用户名不能超过 50 个字符")
    private String newUsername;

    public String getNewUsername() {
        return newUsername;
    }

    public void setNewUsername(String newUsername) {
        this.newUsername = newUsername;
    }
}
