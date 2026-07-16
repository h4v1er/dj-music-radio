package org.example.user.controller;

import lombok.RequiredArgsConstructor;
import org.example.user.common.Result;
import org.example.user.dto.LoginDTO;
import org.example.user.dto.RegisterDTO;
import org.example.user.entity.Favorite;
import org.example.user.entity.PlayHistory;
import org.example.user.entity.User;
import org.example.user.service.FavoriteService;
import org.example.user.service.PlayHistoryService;
import org.example.user.service.UserService;
import org.example.user.utils.JwtUtil;
import org.example.user.vo.LoginVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FavoriteService favoriteService;
    private final PlayHistoryService playHistoryService;
    private final JwtUtil jwtUtil;

    @GetMapping("/hello")
    public String hello() {
        return "👤 用户服务已就绪！";
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterDTO dto) {
        try {
            boolean ok = userService.register(dto);
            return ok ? Result.success() : Result.error("注册失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginDTO dto) {
        try {
            LoginVO vo = userService.login(dto);
            return Result.success(vo);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/info")
    public Result<?> info(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || token.isBlank()) {
            return Result.error("未登录");
        }
        try {
            Long userId = jwtUtil.getUserId(token.replace("Bearer ", ""));
            User user = userService.getUserInfo(userId);
            if (user != null) {
                user.setPassword(null);
            }
            return Result.success(user);
        } catch (Exception e) {
            return Result.error("Token无效");
        }
    }

    @PutMapping("/info")
    public Result<?> updateInfo(@RequestHeader("Authorization") String token,
                                @RequestBody User user) {
        try {
            Long userId = jwtUtil.getUserId(token.replace("Bearer ", ""));
            user.setId(userId);
            user.setPassword(null);
            boolean ok = userService.updateUser(user);
            return ok ? Result.success() : Result.error("更新失败");
        } catch (Exception e) {
            return Result.error("Token无效");
        }
    }

    @PostMapping("/favorite/add")
    public Result<?> addFavorite(@RequestHeader("Authorization") String token,
                                 @RequestParam Long songId) {
        try {
            Long userId = jwtUtil.getUserId(token.replace("Bearer ", ""));
            favoriteService.addFavorite(userId, songId);
            return Result.success();
        } catch (Exception e) {
            return Result.error("Token无效");
        }
    }

    @DeleteMapping("/favorite/{songId}")
    public Result<?> removeFavorite(@RequestHeader("Authorization") String token,
                                    @PathVariable Long songId) {
        try {
            Long userId = jwtUtil.getUserId(token.replace("Bearer ", ""));
            favoriteService.removeFavorite(userId, songId);
            return Result.success();
        } catch (Exception e) {
            return Result.error("Token无效");
        }
    }

    @GetMapping("/favorite/list")
    public Result<?> favoriteList(@RequestHeader("Authorization") String token) {
        try {
            Long userId = jwtUtil.getUserId(token.replace("Bearer ", ""));
            return Result.success(favoriteService.listByUserId(userId));
        } catch (Exception e) {
            return Result.error("Token无效");
        }
    }

    @GetMapping("/history")
    public Result<?> history(@RequestHeader("Authorization") String token) {
        try {
            Long userId = jwtUtil.getUserId(token.replace("Bearer ", ""));
            return Result.success(playHistoryService.listByUserId(userId, 50));
        } catch (Exception e) {
            return Result.error("Token无效");
        }
    }

    @PostMapping("/history/add")
    public Result<?> addHistory(@RequestHeader(value = "Authorization", required = false) String token,
                                @RequestParam Long songId) {
        try {
            Long userId = jwtUtil.getUserId(token.replace("Bearer ", ""));
            playHistoryService.recordPlay(userId, songId);
            return Result.success();
        } catch (Exception e) {
            return Result.error("Token无效");
        }
    }
}
