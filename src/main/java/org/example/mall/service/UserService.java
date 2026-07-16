package org.example.mall.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.mall.dto.LoginDTO;
import org.example.mall.dto.RegisterDTO;
import org.example.mall.entity.User;
import org.example.mall.vo.LoginVO;

public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    boolean register(RegisterDTO dto);

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO dto);

    /**
     * 查询用户信息
     */
    User getUserInfo(Long id);

    /**
     * 修改用户信息
     */
    boolean updateUser(User user);

}