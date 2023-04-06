package br.ufsm.poli.csi.tapw.pilacoin.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import javax.crypto.Cipher;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Utils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    private static final String HASH_ALGORITHM = "SHA-256";
    private static BigInteger dificuldade;

    @SneakyThrows
    public static byte[] getKeyBytes(String path) {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (Exception e) {
            return createKeyPair().getPublic().getEncoded();
        }
    }

    @SneakyThrows
    private static KeyPair createKeyPair() {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();
        Files.write(Paths.get("public_key.der"), keyPair.getPublic().getEncoded());
        Files.write(Paths.get("private_key.der"), keyPair.getPrivate().getEncoded());
        return keyPair;
    }

    @SneakyThrows
    public static KeyPair readKeyPair() {
        byte[] barrPub = Files.readAllBytes(Paths.get("public_key.der"));
        byte[] barrPriv = Files.readAllBytes(Paths.get("private_key.der"));
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(barrPub));
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(barrPriv));
        return new KeyPair(publicKey, privateKey);
    }

    @SneakyThrows
    public static boolean keyPairVerifier(PublicKey publicKey, PrivateKey privateKey) {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        byte[] data = "Hello, World!".getBytes();
        signature.update(data);
        byte[] signedData = signature.sign();

        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(signedData);
    }

    @SneakyThrows
    public static byte[] getHash(Object o) {
        String jsonString = OBJECT_MAPPER.writeValueAsString(o);
        MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
        return md.digest(jsonString.getBytes(StandardCharsets.UTF_8));
    }

    @SneakyThrows
    public static String encrypt(Key key, byte[] objectHash) {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptObject = cipher.doFinal(objectHash);
        return Base64.getEncoder().encodeToString(encryptObject);
    }

    public static BigInteger getDificuldade() {
        return dificuldade;
    }

    public static void setDificuldade(BigInteger dificuldade) {
        Utils.dificuldade = dificuldade;
    }
}
