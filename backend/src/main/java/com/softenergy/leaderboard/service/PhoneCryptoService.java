package com.softenergy.leaderboard.service;

import com.softenergy.leaderboard.config.SoftEnergyProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PhoneCryptoService {
    private static final int IV_LENGTH = 12;
    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec encryptionKey;
    private final SecretKeySpec hmacKey;

    public PhoneCryptoService(SoftEnergyProperties properties) {
        this.encryptionKey = new SecretKeySpec(
                Base64.getDecoder().decode(properties.getSecurity().getPhoneKeyBase64()), "AES");
        this.hmacKey = new SecretKeySpec(
                Base64.getDecoder().decode(properties.getSecurity().getPhoneHmacBase64()), "HmacSHA256");
    }

    public String encrypt(String phone) {
        if (phone == null || phone.isBlank()) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(phone.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("手机号加密失败", exception);
        }
    }

    public String decrypt(String encryptedPhone) {
        if (encryptedPhone == null || encryptedPhone.isBlank()) return null;
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedPhone);
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[payload.length - IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            System.arraycopy(payload, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("手机号解密失败", exception);
        }
    }

    public String hash(String phone) {
        if (phone == null || phone.isBlank()) return null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            return HexFormat.of().formatHex(mac.doFinal(phone.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("手机号索引计算失败", exception);
        }
    }

    public String maskEncrypted(String encryptedPhone) {
        return mask(decrypt(encryptedPhone));
    }

    public String mask(String phone) {
        if (phone == null || phone.length() < 7) return "未授权";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}

