package com.example.nextthingb1;

import android.content.Context;
import android.content.res.AssetManager;
import com.google.gson.Gson;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.time.Instant;

// 注意：此类需放在app模块的java目录下（如 com.yourpackage.util）
public class QWeatherJwtGenerator {
    // 静态初始化块：注册BouncyCastle提供者以支持EdDSA
    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    
    // 1. 替换为你的参数（核心！必须改）
    private static final String PRIVATE_KEY_ASSETS_PATH = "ed25519-private.pem"; // assets中的私钥文件名
    private static final String KID = "TNB27NDAYA"; // 控制台→项目→凭据列表→凭据ID
    private static final String SUB = "2HDXW9HPCN"; // 控制台→项目→项目信息→项目ID
    private static final long EXPIRATION_SECONDS = 3600; // 1小时有效期（最长24小时）

    // 2. 传入Android Context（用于读取assets文件）
    public static String generateJwt(Context context) throws Exception {
        // 步骤1：从assets读取并解析Ed25519私钥
        PrivateKey privateKey = loadEd25519PrivateKeyFromAssets(context, PRIVATE_KEY_ASSETS_PATH);

        // 步骤2：构建Header并Base64URL编码
        Header header = new Header("EdDSA", KID);
        String headerEncoded = base64UrlEncode(new Gson().toJson(header).getBytes(StandardCharsets.UTF_8));

        // 步骤3：构建Payload并Base64URL编码
        long currentTime = Instant.now().getEpochSecond();
        long iat = currentTime - 30; // 减30秒避免服务器时间误差
        long exp = iat + EXPIRATION_SECONDS;
        Payload payload = new Payload(SUB, iat, exp);
        String payloadEncoded = base64UrlEncode(new Gson().toJson(payload).getBytes(StandardCharsets.UTF_8));

        // 步骤4：生成签名（Ed25519私钥签名）
        String dataToSign = headerEncoded + "." + payloadEncoded;
        String signatureEncoded = signWithPrivateKey(dataToSign, privateKey);

        // 步骤5：拼接最终JWT
        return dataToSign + "." + signatureEncoded;
    }

    /**
     * 从Android assets目录读取Ed25519私钥（适配Android）
     * @param context Android上下文（如Activity、Application）
     * @param assetsPath assets中的私钥路径（如 "ed25519-private.pem"）
     * @return Ed25519私钥
     */
    private static PrivateKey loadEd25519PrivateKeyFromAssets(Context context, String assetsPath) throws Exception {
        AssetManager assetManager = context.getAssets();
        StringBuilder pemContent = new StringBuilder();

        // 1. 读取assets中的PEM文件（按行读取，避免编码问题）
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(assetManager.open(assetsPath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 跳过PEM头尾部（如 "-----BEGIN PRIVATE KEY-----"）
                if (line.startsWith("-----BEGIN") || line.startsWith("-----END")) {
                    continue;
                }
                pemContent.append(line.trim()); // 拼接并去除空格
            }
        } catch (IOException e) {
            throw new RuntimeException("读取assets私钥失败：" + e.getMessage(), e);
        }

        // 2. Base64解码PEM内容（得到私钥字节数组）
        byte[] privateKeyBytes = Base64.getDecoder().decode(pemContent.toString());

        // 3. 根据和风天气官方文档，使用EdDSA算法
        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            // 优先尝试BouncyCastle的EdDSA
            KeyFactory keyFactory = KeyFactory.getInstance("EdDSA", "BC");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            // 如果BouncyCastle失败，尝试系统默认的EdDSA
            try {
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("EdDSA");
                return keyFactory.generatePrivate(keySpec);
            } catch (Exception e2) {
                throw new RuntimeException("无法生成Ed25519私钥。根据和风天气官方文档，应该使用EdDSA算法。请检查：1) 私钥格式是否正确 2) BouncyCastle依赖是否正确添加。错误：" + e2.getMessage(), e2);
            }
        }
    }

    /**
     * JWT标准Base64URL编码（替换+为-、/为_，去除=）
     */
    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder()
                .withoutPadding() // 去除末尾的=
                .encodeToString(data);
    }

    /**
     * 用Ed25519私钥签名（使用EdDSA算法）
     */
    private static String signWithPrivateKey(String data, PrivateKey privateKey) throws Exception {
        try {
            // 根据和风天气官方文档，使用EdDSA算法
            Signature signature = Signature.getInstance("EdDSA", "BC");
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = signature.sign();
            return base64UrlEncode(signatureBytes);
        } catch (Exception e) {
            // 如果BouncyCastle失败，尝试系统默认的EdDSA
            try {
                Signature signature = Signature.getInstance("EdDSA");
                signature.initSign(privateKey);
                signature.update(data.getBytes(StandardCharsets.UTF_8));
                byte[] signatureBytes = signature.sign();
                return base64UrlEncode(signatureBytes);
            } catch (Exception e2) {
                throw new RuntimeException("无法使用Ed25519私钥签名。根据和风天气官方文档，应该使用EdDSA算法。请检查BouncyCastle依赖配置。错误：" + e2.getMessage(), e2);
            }
        }
    }

    // 内部类：JWT Header（固定alg=EdDSA，kid=凭据ID）
    static class Header {
        private String alg;
        private String kid;

        public Header(String alg, String kid) {
            this.alg = alg;
            this.kid = kid;
        }

        // FastJSON序列化需要Getter（必须有，否则JSON字段缺失）
        public String getAlg() { return alg; }
        public String getKid() { return kid; }
    }

    // 内部类：JWT Payload（必须包含sub/iat/exp）
    static class Payload {
        private String sub;
        private long iat;
        private long exp;

        public Payload(String sub, long iat, long exp) {
            this.sub = sub;
            this.iat = iat;
            this.exp = exp;
        }

        // FastJSON序列化需要Getter（必须有，否则JSON字段缺失）
        public String getSub() { return sub; }
        public long getIat() { return iat; }
        public long getExp() { return exp; }
    }
}
