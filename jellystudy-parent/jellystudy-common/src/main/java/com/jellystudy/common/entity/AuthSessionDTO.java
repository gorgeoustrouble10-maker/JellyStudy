package com.jellystudy.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthSessionDTO implements Serializable {

    private String token;
    private String userId;
    private String displayName;
    private long expiresInHours;
}
