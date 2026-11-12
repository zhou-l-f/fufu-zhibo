package com.zlf.fufuzhibo.common.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;

@Component
public class CryptoUtil {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${fufu.crypto.aes-key}")
    private String aesKey;

    @Value("${fufu.crypto.hmac-key}")
    private String hmacKey;

    /** AES-GCM 加密，返回 IV(12字节) + 密文 的拼接结果 */
    public byte[] encrypt(String plain) {
        if (plain == null) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(
                    aesKey.getBytes(StandardCharsets.UTF_8), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            byte[] result = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(cipherText, 0, result, iv.length, cipherText.length);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("加密失败", e);
        }
    }

    /** AES-GCM 解密 */
    public String decrypt(byte[] data) {
        if (data == null) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(data, 0, iv, 0, GCM_IV_LENGTH);
            byte[] cipherText = new byte[data.length - GCM_IV_LENGTH];
            System.arraycopy(data, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(
                    aesKey.getBytes(StandardCharsets.UTF_8), "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("解密失败", e);
        }
    }

    /** HMAC-SHA256，返回 64 位十六进制字符串，用于唯一索引去重 */
    public String hmac(String plain) {
        if (plain == null) return null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 计算失败", e);
        }
    }
}