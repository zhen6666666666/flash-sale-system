package com.example.flashsale.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    private String username;
    private String password;
    private String phone;
    private Integer role; // 前端传 0（普通用户）或 1（商家）
}