package org.example.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.user.dto.LoginDTO;
import org.example.user.dto.PasswordDTO;
import org.example.user.dto.RegisterDTO;
import org.example.user.entity.User;
import org.example.user.vo.LoginVO;

public interface UserService extends IService<User> {

    LoginVO login(LoginDTO dto);

    boolean register(RegisterDTO dto);

    User getUserInfo(Long id);

    boolean updateUser(User user);

    boolean changePassword(Long userId, PasswordDTO dto);

    Long authenticate(String authorizationHeader);
}
