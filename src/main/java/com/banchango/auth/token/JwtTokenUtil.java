package com.banchango.auth.token;


import com.banchango.auth.exception.AuthenticateException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class JwtTokenUtil {

    private static final String SECRET_KEY = "secret_key";
    private static final int REFRESH_TOKEN_EXPIRATION = 86400000 * 7;
    private static final int ACCESS_TOKEN_EXPIRATION = 86400000;

    public static String extractUserId(String token) throws AuthenticateException {
        return extractClaim(token, Claims::getSubject);
    }

    private static Date extractExpiration(String token) throws AuthenticateException {
        return extractClaim(token, Claims::getExpiration);
    }

    public static <T> T extractClaim(String token, Function<Claims, T> claimResolver) throws AuthenticateException {
        Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private static Claims extractAllClaims(String token) throws AuthenticateException{
        try {
            return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
        } catch(Exception exception) {
            throw new AuthenticateException();
        }
    }

    public static boolean isTokenExpired(String token) throws AuthenticateException{
        try {
            boolean result = extractExpiration(token).before(new Date());
            return result;
        } catch(Exception exception) {
            throw new AuthenticateException();
        }

    }

    public static String generateAccessToken(Integer userId) {
        Map<String, Object> claims = new HashMap<>();
        return createAccessToken(claims, userId.toString());
    }

    private static String createAccessToken(Map<String, Object> claims, String subject) {
        return Jwts.builder().setClaims(claims).setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    public static String generateRefreshToken(Integer userId) {
        Map<String, Object> claims = new HashMap<>();
        return createRefreshToken(claims, userId.toString());
    }

    private static String createRefreshToken(Map<String, Object> claims, String subject) {
        return Jwts.builder().setClaims(claims).setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    public static boolean validateToken(String token) throws AuthenticateException {
        try {
            String userId = extractUserId(token);
            if(userId == null) throw new Exception();
            return (userId != null && !isTokenExpired(token));
        } catch(Exception exception) {
            throw new AuthenticateException();
        }
    }

    public static String getToken(String bearerToken) throws AuthenticateException{
        String[] splitAuthorization = bearerToken.split(" ");
        String schema = splitAuthorization[0];
        String token = splitAuthorization[1];
        if(!schema.equals("Bearer")) throw new AuthenticateException();
        return token;
    }
}