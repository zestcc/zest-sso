package cn.zest.sso.server.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;

/**
 * RFC 6238 TOTP 实现，用于 MFA 二次验证。
 */
public final class TotpUtil {

    private static final int SECRET_BYTES = 20;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;

    private TotpUtil() {
    }

    public static String generateSecret() {
        byte[] buffer = new byte[SECRET_BYTES];
        new SecureRandom().nextBytes(buffer);
        return Base32.encode(buffer);
    }

    public static String buildOtpAuthUrl(String issuer, String account, String secret) {
        return "otpauth://totp/" + urlEncode(issuer) + ":" + urlEncode(account)
                + "?secret=" + secret + "&issuer=" + urlEncode(issuer) + "&digits=6&period=30";
    }

    public static boolean verify(String base32Secret, String code, int window) {
        if (base32Secret == null || code == null || !code.matches("\\d{6}")) {
            return false;
        }
        long parsedCode = Long.parseLong(code);
        long currentCounter = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        for (int i = -window; i <= window; i++) {
            if (generateCode(base32Secret, currentCounter + i) == parsedCode) {
                return true;
            }
        }
        return false;
    }

    private static long generateCode(String base32Secret, long counter) {
        try {
            byte[] key = Base32.decode(base32Secret);
            byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            return binary % (int) Math.pow(10, CODE_DIGITS);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("TOTP 计算失败", e);
        }
    }

    private static String urlEncode(String value) {
        return value.replace(" ", "%20");
    }

    private static final class Base32 {
        private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

        private Base32() {
        }

        static String encode(byte[] data) {
            StringBuilder result = new StringBuilder((data.length * 8 + 4) / 5);
            int buffer = 0;
            int bitsLeft = 0;
            for (byte b : data) {
                buffer = (buffer << 8) | (b & 0xFF);
                bitsLeft += 8;
                while (bitsLeft >= 5) {
                    result.append(ALPHABET[(buffer >> (bitsLeft - 5)) & 0x1F]);
                    bitsLeft -= 5;
                }
            }
            if (bitsLeft > 0) {
                result.append(ALPHABET[(buffer << (5 - bitsLeft)) & 0x1F]);
            }
            return result.toString();
        }

        static byte[] decode(String encoded) {
            String normalized = encoded.replace("=", "").toUpperCase();
            ByteBuffer buffer = ByteBuffer.allocate(normalized.length() * 5 / 8 + 1);
            int bits = 0;
            int value = 0;
            for (char c : normalized.toCharArray()) {
                int index = indexOf(c);
                if (index < 0) {
                    throw new IllegalArgumentException("非法 Base32 字符");
                }
                value = (value << 5) | index;
                bits += 5;
                if (bits >= 8) {
                    buffer.put((byte) (value >> (bits - 8)));
                    bits -= 8;
                }
            }
            byte[] result = new byte[buffer.position()];
            buffer.flip();
            buffer.get(result);
            return result;
        }

        private static int indexOf(char c) {
            if (c >= 'A' && c <= 'Z') {
                return c - 'A';
            }
            if (c >= '2' && c <= '7') {
                return c - '2' + 26;
            }
            return -1;
        }
    }
}
