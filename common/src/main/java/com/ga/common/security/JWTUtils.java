package com.ga.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JWTUtils {

    @Value("${jwt-secret}")
    private String jwtSecret;

    private Claims parseClaims(String token){
        return Jwts.parserBuilder().setSigningKey(jwtSecret)
                .build().parseClaimsJws(token)
                .getBody();
    }
    public boolean validateJwtToken(String token){
        try {
            parseClaims(token);
            return true;
        }catch (JwtException | IllegalArgumentException e){
            return false;
        }
    }

    public Long getUserIdFromJwtToken(String token){
        return parseClaims(token).get("userId",Long.class);
    }
    public String getRoleFromJwtToken(String token){
        return parseClaims(token).get("role",String.class);
    }
    public String getUsernameFromJwtToken(String token){
        return parseClaims(token).get("username",String.class);
    }

}
