package com.yassine.portfolio_tracker.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String username;
    private String displayName;
    private String email;
    private String token;
}
