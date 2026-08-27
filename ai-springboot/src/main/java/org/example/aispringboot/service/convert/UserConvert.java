package org.example.aispringboot.service.convert;

import org.example.aispringboot.DTO.command.UserRegisterCommandDTO;
import org.example.aispringboot.DTO.response.UserLoginResponseDTO;
import org.example.aispringboot.entity.User;
import org.example.aispringboot.enumClass.UserStatus;

import java.time.LocalDateTime;

public class UserConvert {
    public static UserLoginResponseDTO.UserDetailResponseDTO entityToDetailResponse(User user) {
        return UserLoginResponseDTO.UserDetailResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .gender(user.getGender())
                .genderDisplayName(getGenderDisplayName(user.getGender()))
                .birthday(user.getBirthday())
                .uesrType(user.getUserType())
                .uesrTypeDisplayName(user.getUserTypeDisplayName())
                .status(user.getStatus())
                .statusDisplayName(user.getStatusDisplayName())
                .displayName(user.getDisplayName())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public static UserLoginResponseDTO entityToLoginResponse(String token,UserLoginResponseDTO.UserDetailResponseDTO userInfo)  {
        return UserLoginResponseDTO.builder()
                .userInfo(userInfo)
                .token(token)
                .roleType(userInfo.getUesrType().toString())
                .build();
    }

    public static User registerCommandToEntity(UserRegisterCommandDTO commandDTO,String encodedPassword) {
        return User.builder()
                .username(commandDTO.getUsername())
                .email(commandDTO.getEmail())
                .password(encodedPassword)
                .nickname(commandDTO.getNickname())
                .phone(commandDTO.getPhone())
                .gender(commandDTO.getGender())
                .birthday(commandDTO.getBirthday())
                .userType(commandDTO.getUserType())
                .status(UserStatus.NORMAL.getCode())
//                .displayName(commandDTO.getUsername())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }


    /**
     * 获取性别显示名称
     * @param gender 性别代码
     * @return 性别显示名称
     */
    private static String getGenderDisplayName(Integer gender) {
        if (gender == null) {
            return "未知";
        }
        switch (gender) {
            case 1:
                return "男";
            case 2:
                return "女";
            default:
                return "未知";
        }
    }
}
