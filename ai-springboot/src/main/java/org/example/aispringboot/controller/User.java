package org.example.aispringboot.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.aispringboot.DTO.command.UserLoginCommandDTO;
import org.example.aispringboot.DTO.command.UserPasswordUpdateCommandDTO;
import org.example.aispringboot.DTO.command.UserRegisterCommandDTO;
import org.example.aispringboot.DTO.command.UserUpdateCommandDTO;
import org.example.aispringboot.DTO.response.UserLoginResponseDTO;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.service.UserService;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class User {
    @Resource
    private UserService userService;

    //用户登录接口
    @PostMapping("/login")
    public Result<UserLoginResponseDTO> login(@Valid @RequestBody UserLoginCommandDTO commandDTO) {
        //调用服务层登录方法
        UserLoginResponseDTO result = userService.login(commandDTO);
        return Result.ok(result);
    }

    //用户注册接口
    @PostMapping("/add")
    public Result<UserLoginResponseDTO.UserDetailResponseDTO>  register(@Valid @RequestBody UserRegisterCommandDTO commandDTO){
        //调用服务层注册方法
        UserLoginResponseDTO.UserDetailResponseDTO result = userService.register(commandDTO);
        return Result.ok(result);
    }

    // 获取当前用户
    @GetMapping("/current")
    public Result<UserLoginResponseDTO.UserDetailResponseDTO> getCurrentUser() {
        // 如何从token中解析出用户的id
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
// 调用service层获取用户详情
        UserLoginResponseDTO.UserDetailResponseDTO result = userService.getUserById(userId);
        return Result.ok(result);
    }

    // 更新用户资料
    @PutMapping("/profile")
    public Result<UserLoginResponseDTO.UserDetailResponseDTO> updateProfile(@Valid @RequestBody UserUpdateCommandDTO commandDTO) {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        UserLoginResponseDTO.UserDetailResponseDTO result = userService.updateProfile(userId, commandDTO);
        return Result.ok(result);
    }

    // 修改密码
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody UserPasswordUpdateCommandDTO commandDTO) {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        userService.updatePassword(userId, commandDTO);
        return Result.ok();
    }

}