package com.inventories.infr.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

@Component
@Slf4j
public class JwtUtils {

    @Value("${spring.secret.key}")
    private String secretKey;

    @Value("${spring.time.expiration}")
    private String timeExpiration;

    public String generateToken(String username){
        System.out.println("Generando token");
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + Long.parseLong(timeExpiration)))
                .signWith(getSignatureKey(), Jwts.SIG.HS256)
                .compact();
    }
    public SecretKey getSignatureKey(){
        byte [] key = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(key);
    }

    public boolean isValidToken(String token){
        try {
            Jwts.parser()
                    .verifyWith(getSignatureKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    public String getUserFromToken(String token){
        return getClaims(token, Claims::getSubject);
    }

    public <T> T getClaims(String token, Function<Claims, T> claimTFunction){
        Claims claims = extractAllClaims(token);
        return claimTFunction.apply(claims);
    }

    public Claims extractAllClaims(String token){
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(getSignatureKey())
                .build()
                .parseSignedClaims(token);

        return jws.getPayload();
    }


}
