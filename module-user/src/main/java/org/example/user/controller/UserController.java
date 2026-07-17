package org.example.user.controller;

import org.example.user.common.Result;
import org.example.user.dto.LoginDTO;
import org.example.user.dto.RegisterDTO;
import org.example.user.entity.User;
import org.example.user.service.MusicLibraryService;
import org.example.user.service.UserService;
import org.example.user.vo.LoginVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final MusicLibraryService musicLibraryService;

    public UserController(UserService userService, MusicLibraryService musicLibraryService) {
        this.userService = userService;
        this.musicLibraryService = musicLibraryService;
    }

    @GetMapping("/hello")
    public String hello() {
        return "用户服务已就绪";
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterDTO dto) {
        try {
            userService.register(dto);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginDTO dto) {
        try {
            LoginVO vo = userService.login(dto);
            return Result.success(vo);
        } catch (RuntimeException e) {
            return Result.error(401, e.getMessage());
        }
    }

    @GetMapping("/info")
    public Result<?> info(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = userService.authenticate(token);
            User user = userService.getUserInfo(userId);
            if (user != null) {
                user.setPasswordHash(null);
            }
            return Result.success(user);
        } catch (RuntimeException e) {
            return Result.error(401, e.getMessage());
        }
    }

    @PutMapping("/info")
    public Result<?> updateInfo(@RequestHeader(value = "Authorization", required = false) String token,
                                @RequestBody User user) {
        try {
            Long userId = userService.authenticate(token);
            user.setId(userId);
            return userService.updateUser(user) ? Result.success() : Result.error("更新失败");
        } catch (RuntimeException e) {
            return Result.error(401, e.getMessage());
        }
    }

    @PostMapping("/favorite/add")
    public Result<?> addFavoriteCompat(@RequestHeader(value = "Authorization", required = false) String token,
                                       @RequestParam Long songId) {
        return addFavorite(token, songId);
    }

    @PostMapping("/favorite/{songId}")
    public Result<?> addFavorite(@RequestHeader(value = "Authorization", required = false) String token,
                                 @PathVariable Long songId) {
        try {
            Long userId = userService.authenticate(token);
            musicLibraryService.addFavorite(userId, songId);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/favorite/{songId}")
    public Result<?> removeFavorite(@RequestHeader(value = "Authorization", required = false) String token,
                                    @PathVariable Long songId) {
        try {
            Long userId = userService.authenticate(token);
            musicLibraryService.removeFavorite(userId, songId);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @GetMapping("/favorite/list")
    public Result<?> favoriteList(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = userService.authenticate(token);
            return Result.success(musicLibraryService.favoriteList(userId));
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @GetMapping("/history")
    public Result<?> history(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = userService.authenticate(token);
            return Result.success(musicLibraryService.historyList(userId));
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/history/add")
    public Result<?> addHistoryCompat(@RequestHeader(value = "Authorization", required = false) String token,
                                      @RequestParam Long songId) {
        return addHistory(token, Map.of("songId", songId));
    }

    @PostMapping("/history")
    public Result<?> addHistory(@RequestHeader(value = "Authorization", required = false) String token,
                                @RequestBody Map<String, Long> body) {
        try {
            Long songId = body.get("songId");
            if (songId == null) {
                return Result.error(400, "songId 不能为空");
            }
            Long userId = userService.authenticate(token);
            musicLibraryService.recordHistory(userId, songId);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }
}
