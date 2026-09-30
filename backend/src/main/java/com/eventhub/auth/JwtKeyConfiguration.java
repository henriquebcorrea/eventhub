package com.eventhub.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
class JwtKeyConfiguration {
    @Bean
    KeyPair jwtKeyPair(@Value("${app.jwt.private-key:}") String privateKey,
                       @Value("${app.jwt.public-key:}") String publicKey,
                       @Value("${app.jwt.allow-ephemeral:false}") boolean allowEphemeral) throws Exception {
        if (!privateKey.isBlank() && !publicKey.isBlank()) {
            var factory = KeyFactory.getInstance("RSA");
            var privateBytes = Base64.getDecoder().decode(stripPem(privateKey));
            var publicBytes = Base64.getDecoder().decode(stripPem(publicKey));
            return new KeyPair(factory.generatePublic(new X509EncodedKeySpec(publicBytes)),
                    factory.generatePrivate(new PKCS8EncodedKeySpec(privateBytes)));
        }
        if (!allowEphemeral) throw new IllegalStateException("Configure JWT_PRIVATE_KEY e JWT_PUBLIC_KEY para iniciar a API.");
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @Bean
    JwtEncoder jwtEncoder(KeyPair pair) {
        var key = new RSAKey.Builder((RSAPublicKey) pair.getPublic()).privateKey((RSAPrivateKey) pair.getPrivate()).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }

    @Bean
    JwtDecoder jwtDecoder(KeyPair pair, @Value("${app.jwt.issuer}") String issuer) {
        var decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) pair.getPublic()).build();
        OAuth2TokenValidator<Jwt> audience = token -> token.getAudience().contains("eventhub-api")
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Audience inválida.", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), audience));
        return decoder;
    }

    private static String stripPem(String value) {
        return value.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }
}

