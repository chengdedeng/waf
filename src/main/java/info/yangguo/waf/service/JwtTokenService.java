package info.yangguo.waf.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Optional;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtTokenService {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);

    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.expireAfterMin}")
    private Integer jwtExpireAfter;


    public String genToken(Map<String, String> claims) {
        try {
            Algorithm algorithmHS = buildHMAC256();
            JWTCreator.Builder jwtBuilder = JWT.create();
            jwtBuilder.withSubject("waf-token")
                    .withIssuer("yangguo.info");
            //Set token expire time
            if (Optional.fromNullable(jwtExpireAfter).isPresent()) {
                DateTime nowTime = DateTime.now();
                DateTime newTime = nowTime.plusMinutes(jwtExpireAfter);
                jwtBuilder.withExpiresAt(newTime.toDate());
            }
            claims.entrySet().stream().forEach(claim -> {
                jwtBuilder.withClaim(claim.getKey(), String.valueOf(claim.getValue()));
            });
            return jwtBuilder.sign(algorithmHS);
        } catch (Exception e) {
            logger.error(ExceptionUtils.getFullStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> getTokenPlayload(String token) {
        Map map = new HashMap();
        try {
            Algorithm algorithmHS = buildHMAC256();
            //When the token is not valid , this method will throw SignatureVerificationException
            DecodedJWT jwt = JWT.require(algorithmHS).acceptLeeway(3).build().verify(token);
            Map<String, Claim> claimMap = jwt.getClaims();
            if (Optional.fromNullable(claimMap).isPresent()) {
                claimMap.entrySet().stream().forEach(entry -> {
                    map.put(entry.getKey(), entry.getValue().as(String.class));
                });
            }
        } catch (Exception e) {
            logger.error(ExceptionUtils.getFullStackTrace(e));
            throw new RuntimeException(e);
        }
        return map;
    }

    public boolean verifyToken(String token) {
        try {
            Algorithm algorithmHS = buildHMAC256();
            //When the token is not valid , this method will throw SignatureVerificationException
            JWT.require(algorithmHS).acceptLeeway(3).build().verify(token);
            return true;
        } catch (TokenExpiredException e) {
            logger.info("Token:[{}] is expired", token);
        } catch (Exception e) {
            logger.warn("Token:[{}] is illegal", token);
        }
        return false;
    }

    private Algorithm buildHMAC256() throws UnsupportedEncodingException {
        //Keep the HMAC256 Secret in cache , escape the secret is disclosure
        return Algorithm.HMAC256(jwtSecret);
    }
}