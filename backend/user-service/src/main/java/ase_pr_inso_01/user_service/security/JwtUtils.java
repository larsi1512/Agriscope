package ase_pr_inso_01.user_service.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {


  @Value("${app.jwt.secret}")
  private String jwtSecret;

  @Value("${app.jwt.expiration}")
  private long jwtExpirationMs;

  public String generateToken(String username) {
    return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()), SignatureAlgorithm.HS512)
            .compact();
  }

  public String getUsernameFromJwt(String token) {
    return Jwts.parser().setSigningKey(getSigningKey())
            .parseClaimsJws(token)
            .getBody().getSubject();
  }

  public boolean validateJwtToken(String token) {
    try {
      Jwts.parser().setSigningKey(getSigningKey()).parseClaimsJws(token);
      return true;
    } catch (JwtException e) {
      return false;
    }
  }

  public String generatePasswordResetToken(String username) {
    long resetExpirationMs = 900000;
    return Jwts.builder()
            .setSubject(username)
            .claim("purpose", "password_reset")
            .setIssuedAt(new Date())
            .setExpiration(new Date((new Date()).getTime() + resetExpirationMs))
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact();
  }

  private Key getSigningKey() {
    return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }
}
