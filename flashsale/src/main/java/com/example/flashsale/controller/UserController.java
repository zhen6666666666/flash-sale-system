package com.example.flashsale.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.flashsale.dto.LoginDTO;
import com.example.flashsale.dto.RegisterDTO;
import com.example.flashsale.entity.User;
import com.example.flashsale.mapper.UserMapper;
import com.example.flashsale.utils.R;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserMapper userMapper;

    /**
     * 1. 统一登录接口
     */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody LoginDTO loginDTO) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, loginDTO.getUsername())
        );

        if (user == null || !user.getPassword().equals(loginDTO.getPassword())) {
            return R.error("用户名或密码错误");
        }

        String token = JWT.create()
                .withClaim("userId", user.getId())
                .withClaim("username", user.getUsername())
                .withClaim("role", user.getRole())
                .withExpiresAt(new Date(System.currentTimeMillis() + 24 * 3600 * 1000))
                .sign(Algorithm.HMAC256("flashsale_secret"));

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("phone", user.getPhone());
        result.put("avatar", user.getAvatar()); // 🌟 返回头像URL

        return R.success("登录成功", result);
    }

    /**
     * 2. 注册接口
     */
    @PostMapping("/register")
    public R<String> register(@RequestBody RegisterDTO registerDTO) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, registerDTO.getUsername())
        );
        if (count > 0) {
            return R.error("该用户名已被注册");
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(registerDTO.getPassword());
        user.setPhone(registerDTO.getPhone());
        Integer role = (registerDTO.getRole() != null && registerDTO.getRole() == 1) ? 1 : 0;
        user.setRole(role);
        user.setCreateTime(new Date());

        userMapper.insert(user);
        return R.success(role == 1 ? "商家账号注册成功！" : "用户注册成功！");
    }

    /**
     * 3. 根据 userId 获取最新用户信息
     */
    @GetMapping("/info")
    public R<User> getUserInfo(@RequestParam("userId") Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return R.error("用户不存在");
        }
        user.setPassword(null);
        return R.success("获取成功", user);
    }

    /**
     * 4. 头像文件上传接口
     */
    @PostMapping("/uploadAvatar")
    public R<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.error("上传文件不能为空");
        }

        String uploadDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = (originalFilename != null && originalFilename.contains(".")) ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".png";
        String newFilename = UUID.randomUUID().toString() + suffix;

        try {
            file.transferTo(new File(uploadDir + newFilename));
            String avatarUrl = "/api/uploads/" + newFilename;
            return R.success("头像上传成功", avatarUrl);
        } catch (IOException e) {
            return R.error("头像文件保存失败: " + e.getMessage());
        }
    }

    /**
     * 5. 更新用户资料
     */
    @PostMapping("/update")
    public R<String> updateProfile(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("id").toString());
        String username = (String) params.get("username");
        String phone = (String) params.get("phone");
        String avatar = (String) params.get("avatar");
        String oldPassword = (String) params.get("oldPassword");
        String newPassword = (String) params.get("newPassword");

        User user = userMapper.selectById(userId);
        if (user == null) {
            return R.error("用户不存在");
        }

        // 修改密码校验
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (oldPassword == null || !oldPassword.equals(user.getPassword())) {
                return R.error("原密码验证错误");
            }
            user.setPassword(newPassword);
        }

        user.setUsername(username);
        user.setPhone(phone);
        if (avatar != null) {
            user.setAvatar(avatar);
        }

        userMapper.updateById(user);
        return R.success("个人资料修改成功");
    }
}