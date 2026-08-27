package org.example.aispringboot.DTO.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateCommandDTO {
    // 昵称
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    // 邮箱
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    // 手机号
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    // 头像
    @Size(max = 255, message = "头像路径长度不能超过255个字符")
    private String avatar;

    // 性别
    private Integer gender;

    // 生日
    private String birthday;
}
