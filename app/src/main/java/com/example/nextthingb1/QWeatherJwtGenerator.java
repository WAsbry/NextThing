package com.example.nextthingb1;

import android.content.Context;
import android.content.res.AssetManager;
import com.google.gson.Gson;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.util.encoders.Base64;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import android.util.Log;

public class QWeatherJwtGenerator {

    private static final String PRIVATE_KEY_ASSETS_PATH = "ed25519-private.pem";
    private static final String KID = "T85DFFFK2W";
    private static final String SUB = "3KDX498DD3";
    private static final long EXPIRATION_SECONDS = 3600;

    public static String generateJwt(Context context) throws Exception {
        Ed25519PrivateKeyParameters privateKey = loadEd25519PrivateKeyFromAssets(context, PRIVATE_KEY_ASSETS_PATH);

        Header header = new Header("EdDSA", KID);
        String headerEncoded = base64UrlEncode(new Gson().toJson(header).getBytes(StandardCharsets.UTF_8));

        long currentTime = Instant.now().getEpochSecond();
        long iat = currentTime - 30;
        long exp = iat + EXPIRATION_SECONDS;
        Payload payload = new Payload(SUB, iat, exp);

        // 详细日志
        Log.d("QWeatherJWT", "📊 JWT Payload详情:");
        Log.d("QWeatherJWT", "   - SUB (项目ID): " + SUB);
        Log.d("QWeatherJWT", "   - IAT (签发时间): " + iat + " (" + new java.util.Date(iat * 1000) + ")");
        Log.d("QWeatherJWT", "   - EXP (过期时间): " + exp + " (" + new java.util.Date(exp * 1000) + ")");
        Log.d("QWeatherJWT", "   - 有效时长: " + EXPIRATION_SECONDS + "秒");

        String payloadJson = new Gson().toJson(payload);
        Log.d("QWeatherJWT", "   - Payload JSON: " + payloadJson);
        String payloadEncoded = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

        String dataToSign = headerEncoded + "." + payloadEncoded;
        String signatureEncoded = signWithPrivateKey(dataToSign, privateKey);

        return dataToSign + "." + signatureEncoded;
    }

    private static Ed25519PrivateKeyParameters loadEd25519PrivateKeyFromAssets(Context context, String assetsPath) throws Exception {
        AssetManager assetManager = context.getAssets();
        StringBuilder pemContent = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(assetManager.open(assetsPath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("-----BEGIN") || line.startsWith("-----END")) {
                    continue;
                }
                pemContent.append(line.trim());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read private key from assets: " + e.getMessage(), e);
        }

        String base64Content = pemContent.toString();
        Log.d("QWeatherJWT", "Read base64 content: " + base64Content);
        Log.d("QWeatherJWT", "Base64 content length: " + base64Content.length());
        
        // Debug: print first and last few characters
        if (base64Content.length() > 0) {
            Log.d("QWeatherJWT", "First 10 chars: " + base64Content.substring(0, Math.min(10, base64Content.length())));
            Log.d("QWeatherJWT", "Last 10 chars: " + base64Content.substring(Math.max(0, base64Content.length() - 10)));
        }

        byte[] privateKeyBytes = Base64.decode(base64Content);
        Log.d("QWeatherJWT", "Decoded key length: " + privateKeyBytes.length);

        try {
            if (privateKeyBytes.length == 48) {
                byte[] rawKey = new byte[32];
                System.arraycopy(privateKeyBytes, 16, rawKey, 0, 32);
                return new Ed25519PrivateKeyParameters(rawKey, 0);
            } else if (privateKeyBytes.length == 32) {
                return new Ed25519PrivateKeyParameters(privateKeyBytes, 0);
            } else {
                throw new RuntimeException("Invalid Ed25519 private key length: " + privateKeyBytes.length + " bytes. Expected 32 (raw) or 48 (PKCS#8) bytes.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot generate Ed25519 private key. Please check: 1) Private key format is correct 2) Private key is 32-byte Ed25519 format. Error: " + e.getMessage(), e);
        }
    }

    private static String base64UrlEncode(byte[] data) {
        return java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(data);
    }

    private static String signWithPrivateKey(String data, Ed25519PrivateKeyParameters privateKey) throws Exception {
        try {
            Ed25519Signer signer = new Ed25519Signer();
            signer.init(true, privateKey);
            signer.update(data.getBytes(StandardCharsets.UTF_8), 0, data.length());
            byte[] signatureBytes = signer.generateSignature();
            return base64UrlEncode(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("Cannot sign with Ed25519 private key. Please check algorithm support. Error: " + e.getMessage(), e);
        }
    }

    static class Header {
        private String alg;
        private String kid;

        public Header(String alg, String kid) {
            this.alg = alg;
            this.kid = kid;
        }

        public String getAlg() { return alg; }
        public String getKid() { return kid; }
    }

    static class Payload {
        private String sub;
        private long iat;
        private long exp;

        public Payload(String sub, long iat, long exp) {
            this.sub = sub;
            this.iat = iat;
            this.exp = exp;
        }

        public String getSub() { return sub; }
        public long getIat() { return iat; }
        public long getExp() { return exp; }
    }
}