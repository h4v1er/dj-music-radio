package org.example.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.user.dto.LoginDTO;
import org.example.user.dto.PasswordDTO;
import org.example.user.dto.RegisterDTO;
import org.example.user.entity.User;
import org.example.user.mapper.UserMapper;
import org.example.user.service.UserService;
import org.example.user.utils.JwtUtil;
import org.example.user.vo.LoginVO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        validateLogin(dto);
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername().trim()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }

        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.createToken(user.getId(), user.getUsername()));
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        return vo;
    }

    @Override
    public boolean register(RegisterDTO dto) {
        validateRegister(dto);
        String username = dto.getUsername().trim();
        User existing = getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(isBlank(dto.getNickname()) ? username : dto.getNickname().trim());
        user.setPhone(trimToNull(dto.getPhone()));
        user.setEmail(trimToNull(dto.getEmail()));
        user.setCreateTime(now);
        user.setUpdateTime(now);
        return save(user);
    }

    @Override
    public User getUserInfo(Long id) {
        return getById(id);
    }

    @Override
    public boolean updateUser(User user) {
        user.setUsername(null);
        user.setPasswordHash(null);
        user.setCreateTime(null);
        user.setUpdateTime(LocalDateTime.now());
        return updateById(user);
    }

    @Override
    public boolean changePassword(Long userId, PasswordDTO dto) {
        validatePasswordChange(dto);
        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            throw new RuntimeException("原密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        return updateById(user);
    }

    @Override
    public Long authenticate(String authorizationHeader) {
        if (isBlank(authorizationHeader)) {
            throw new RuntimeException("未登录");
        }
        String token = authorizationHeader.replace("Bearer ", "").trim();
        return jwtUtil.getUserId(token);
    }

    private void validateLogin(LoginDTO dto) {
        if (dto == null) {
            throw new RuntimeException("用户名和密码不能为空");
        }
        validateLogin(dto.getUsername(), dto.getPassword());
    }

    private void validateLogin(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            throw new RuntimeException("用户名和密码不能为空");
        }
    }

    private void validateRegister(RegisterDTO dto) {
        if (dto == null) {
            throw new RuntimeException("用户名和密码不能为空");
        }
        validateLogin(dto.getUsername(), dto.getPassword());
        if (dto.getUsername().trim().length() < 3) {
            throw new RuntimeException("用户名至少 3 个字符");
        }
        if (dto.getPassword().length() < 6) {
            throw new RuntimeException("密码至少 6 个字符");
        }
    }

    private void validatePasswordChange(PasswordDTO dto) {
        if (dto == null || isBlank(dto.getOldPassword()) || isBlank(dto.getNewPassword())) {
            throw new RuntimeException("原密码和新密码不能为空");
        }
        if (dto.getNewPassword().length() < 6) {
            throw new RuntimeException("新密码至少 6 个字符");
        }
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw new RuntimeException("新密码不能和原密码相同");
        }
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
