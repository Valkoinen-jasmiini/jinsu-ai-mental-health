package org.example.aispringboot.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.aispringboot.DTO.command.UserLoginCommandDTO;
import org.example.aispringboot.DTO.command.UserPasswordUpdateCommandDTO;
import org.example.aispringboot.DTO.command.UserRegisterCommandDTO;
import org.example.aispringboot.DTO.command.UserUpdateCommandDTO;
import org.example.aispringboot.DTO.response.UserLoginResponseDTO;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.entity.User;
import org.example.aispringboot.enumClass.UserType;
import org.example.aispringboot.exception.BusinessException;
import org.example.aispringboot.mapper.UserMapper;
import org.example.aispringboot.service.convert.UserConvert;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Resource
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserLoginResponseDTO login(UserLoginCommandDTO commandDTO) {
        // 构建查询条件
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, commandDTO.getUsername())
                .or()
                .eq(User::getEmail, commandDTO.getUsername());
        // 调用MP API查询用户
        User user = userMapper.selectOne(queryWrapper);
        System.out.println(user);

        // 判断用户是否存在
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 验证密码
        String inputPassword = commandDTO.getPassword().trim();
        if (!passwordEncoder.matches(inputPassword, user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        // 检查用户状态
        if (!user.isActive()) {
            throw new BusinessException("用户已被禁用,请联系管理员");
        }

        // 生成token
        String token = JwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getUserType());
        System.out.println(token);
        UserLoginResponseDTO.UserDetailResponseDTO userInfo = UserConvert.entityToDetailResponse(user);
        return UserConvert.entityToLoginResponse(token, userInfo);
    }

    public UserLoginResponseDTO.UserDetailResponseDTO register(UserRegisterCommandDTO commandDTO) {
        System.out.println(JSONUtil.parseObj(commandDTO));
        // 验证密码是否一致
        if (!commandDTO.getPassword().equals(commandDTO.getConfirmPassword())) {
            throw new BusinessException("两次输入密码不一致");
        }

        // 检查用户名是否存在
        LambdaQueryWrapper<User> userNameQuery = new LambdaQueryWrapper<>();
        userNameQuery.eq(User::getUsername, commandDTO.getUsername());
        if (userMapper.selectCount(userNameQuery) > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 检查邮箱是否存在
        LambdaQueryWrapper<User> emailQuery = new LambdaQueryWrapper<>();
        emailQuery.eq(User::getEmail, commandDTO.getEmail());
        if (userMapper.selectCount(emailQuery) > 0) {
            throw new BusinessException("该邮箱已被注册,请换一个或直接登录");
        }

        // 检查手机号是否存在(非空才校验,UNIQUE INDEX 允许 NULL 重复)
        if (commandDTO.getPhone() != null && !commandDTO.getPhone().trim().isEmpty()) {
            LambdaQueryWrapper<User> phoneQuery = new LambdaQueryWrapper<>();
            phoneQuery.eq(User::getPhone, commandDTO.getPhone().trim());
            if (userMapper.selectCount(phoneQuery) > 0) {
                throw new BusinessException("该手机号已被注册,请换一个或直接登录");
            }
        }

        // 昵称默认补为用户名(若留空)
        if (commandDTO.getNickname() == null || commandDTO.getNickname().trim().isEmpty()) {
            commandDTO.setNickname(commandDTO.getUsername());
        }

        // 用户类型
        if (!UserType.isValidCode(commandDTO.getUserType())) {
            throw new BusinessException("无效的用户类型");
        }

        // 创建用户
        String password = commandDTO.getPassword().trim();
        String encodedPassword = passwordEncoder.encode(password);
        User user = UserConvert.registerCommandToEntity(commandDTO, encodedPassword);

        // 插入数据库
        userMapper.insert(user);
        return UserConvert.entityToDetailResponse(user);
    }

    public UserLoginResponseDTO.UserDetailResponseDTO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return UserConvert.entityToDetailResponse(user);
    }

    /**
     * 更新用户资料
     */
    public UserLoginResponseDTO.UserDetailResponseDTO updateProfile(Long userId, UserUpdateCommandDTO commandDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 更新昵称
        if (commandDTO.getNickname() != null) {
            user.setNickname(commandDTO.getNickname());
        }
        // 更新邮箱（检查唯一性）
        if (commandDTO.getEmail() != null && !commandDTO.getEmail().equals(user.getEmail())) {
            LambdaQueryWrapper<User> emailQuery = new LambdaQueryWrapper<>();
            emailQuery.eq(User::getEmail, commandDTO.getEmail());
            if (userMapper.selectCount(emailQuery) > 0) {
                throw new BusinessException("该邮箱已被注册");
            }
            user.setEmail(commandDTO.getEmail());
        }
        // 更新手机号（检查唯一性）
        if (commandDTO.getPhone() != null && !commandDTO.getPhone().equals(user.getPhone())) {
            LambdaQueryWrapper<User> phoneQuery = new LambdaQueryWrapper<>();
            phoneQuery.eq(User::getPhone, commandDTO.getPhone());
            if (userMapper.selectCount(phoneQuery) > 0) {
                throw new BusinessException("该手机号已被注册");
            }
            user.setPhone(commandDTO.getPhone());
        }
        // 更新头像
        if (commandDTO.getAvatar() != null) {
            user.setAvatar(commandDTO.getAvatar());
        }
        // 更新性别
        if (commandDTO.getGender() != null) {
            user.setGender(commandDTO.getGender());
        }
        // 更新生日
        if (commandDTO.getBirthday() != null && !commandDTO.getBirthday().isEmpty()) {
            user.setBirthday(java.time.LocalDate.parse(commandDTO.getBirthday()));
        }

        userMapper.updateById(user);
        return UserConvert.entityToDetailResponse(user);
    }

    /**
     * 修改密码
     */
    public void updatePassword(Long userId, UserPasswordUpdateCommandDTO commandDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证原密码
        if (!passwordEncoder.matches(commandDTO.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        // 验证新密码和确认密码一致
        if (!commandDTO.getNewPassword().equals(commandDTO.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }

        // 更新密码
        String encodedPassword = passwordEncoder.encode(commandDTO.getNewPassword());
        user.setPassword(encodedPassword);
        userMapper.updateById(user);
    }

}