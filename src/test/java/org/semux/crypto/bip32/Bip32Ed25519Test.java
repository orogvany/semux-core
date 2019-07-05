/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto.bip32;

import static org.junit.Assert.assertEquals;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;
import org.junit.Assert;
import org.junit.Test;
import org.semux.crypto.CryptoException;
import org.semux.crypto.Hex;
import org.semux.crypto.bip32.key.HdPublicKey;
import org.semux.crypto.bip32.key.KeyVersion;
import org.semux.crypto.bip39.Language;
import org.semux.crypto.bip39.MnemonicGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.SignatureException;

public class Bip32Ed25519Test {

    private static final Logger logger = LoggerFactory.getLogger(Bip32Ed25519Test.class);

    private byte[] SEED = new MnemonicGenerator().getSeedFromWordlist(
            "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
            "",
            Language.ENGLISH);

    private HdKeyGenerator generator = new HdKeyGenerator();
    private HdKeyPair root = generator.getMasterKeyPairFromSeed(SEED, KeyVersion.MAINNET, CoinType.SEMUX);

    @Test
    public void testRoot() {
        String seed = "5eb00bbddcf069084889a8ab9155568165f5c453ccb85e70811aaed6f6da5fc19a5ac40b389cd370d086206dec8aa6c43daea6690f20ad3d8d48b2d2ce9e38e4";
        String kL = "402b03cd9c8bed9ba9f9bd6cd9c315ce9fcc59c7c25d37c85a36096617e69d41";
        String kR = "8e35cb4a3b737afd007f0688618f21a8831643c0e6c77fc33c06026d2a0fc938";
        String A = "291ea7aa3766cd26a3a8688375aa07b3fed73c13d42543a9f19a48dc8b6bfd07";
        String c = "32596435e70647d7d98ef102a32ea40319ca8fb6c851d7346d3bd8f9d1492658";

        logger.info("k = " + Hex.encode(root.getPrivateKey().getKeyData()));
        logger.info("A = " + Hex.encode(root.getPublicKey().getKeyData()));
        logger.info("c = " + Hex.encode(root.getPublicKey().getChainCode()));

        assertEquals(seed, Hex.encode(SEED));
        assertEquals(kL + kR, Hex.encode(root.getPrivateKey().getKeyData()));
        assertEquals(A, Hex.encode(root.getPublicKey().getKeyData()));
        assertEquals(c, Hex.encode(root.getPublicKey().getChainCode()));
    }

    @Test
    public void testOne() {
        // path = "42'/1/2";
        String kL = "b02160bb753c495687eb0b0e0628bf637e85fd3aadac109847afa2ad20e69d41";
        String kR = "00ea111776aabeb85446b186110f8337a758681c96d5d01d5f42d34baf97087b";
        String A = "bc738b13faa157ce8f1534ddd9299e458be459f734a5fa17d1f0e73f559a69ee";
        String c = "c52916b7bb856bd1733390301cdc22fd2b0d5e6fab9908d55fd1bed13bccbb36";

        HdKeyPair child1 = generator.getChildKeyPair(root, 42, true);
        HdKeyPair child2 = generator.getChildKeyPair(child1, 1, false);
        HdKeyPair child3 = generator.getChildKeyPair(child2, 2, false);

        logger.info("k = " + Hex.encode(child3.getPrivateKey().getKeyData()));
        logger.info("A = " + Hex.encode(child3.getPublicKey().getKeyData()));
        logger.info("c = " + Hex.encode(child3.getPublicKey().getChainCode()));

        assertEquals(kL + kR, Hex.encode(child3.getPrivateKey().getKeyData()));
        assertEquals(A, Hex.encode(child3.getPublicKey().getKeyData()));
        assertEquals(c, Hex.encode(child3.getPublicKey().getChainCode()));
    }

    @Test
    public void testTwo() {
        // path = "42'/3'/5";
        String kL = "78164270a17f697b57f172a7ac58cfbb95e007fdcd968c8c6a2468841fe69d41";
        String kR = "15c846a5d003f7017374d12105c25930a2bf8c386b7be3c470d8226f3cad8b6b";
        String A = "286b8d4ef3321e78ecd8e2585e45cb3a8c97d3f11f829860ce461df992a7f51c";
        String c = "7e64c416800883256828efc63567d8842eda422c413f5ff191512dfce7790984";

        HdKeyPair child1 = generator.getChildKeyPair(root, 42, true);
        HdKeyPair child2 = generator.getChildKeyPair(child1, 3, true);
        HdKeyPair child3 = generator.getChildKeyPair(child2, 5, false);

        logger.info("k = " + Hex.encode(child3.getPrivateKey().getKeyData()));
        logger.info("A = " + Hex.encode(child3.getPublicKey().getKeyData()));
        logger.info("c = " + Hex.encode(child3.getPublicKey().getChainCode()));

        assertEquals(kL + kR, Hex.encode(child3.getPrivateKey().getKeyData()));
        assertEquals(A, Hex.encode(child3.getPublicKey().getKeyData()));
        assertEquals(c, Hex.encode(child3.getPublicKey().getChainCode()));
    }

    @Test
    public void testChildKeyGeneration() throws UnsupportedEncodingException {

        HdKeyPair key = generator.getChildKeyPair(root, 0, false);


        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("ed25519");

        EdDSAPrivateKey sk = new EdDSAPrivateKey(new EdDSAPrivateKeySpec(spec, key.getPrivateKey().getKeyData()));
        EdDSAPublicKey pk = new EdDSAPublicKey(new EdDSAPublicKeySpec(key.getPublicKey().getKeyData(), spec));

        String test = "Here's a message";

        byte[] sig = sign(sk, test.getBytes());
        boolean verified = verify(test.getBytes(), sig, pk);

        Assert.assertTrue(verified);
    }

    @Test
    public void testChildChainGeneration() {
        HdKeyPair key = generator.getChildKeyPair(root, 0, false);

        // child key /0/0
        HdPublicKey childPublicKey = generator.getChildPublicKey(key.getPublicKey(),0, false,  Scheme.BIP32_ED25519);
        childPublicKey = generator.getChildPublicKey(childPublicKey,0, false, Scheme.BIP32_ED25519);

        HdKeyPair childKey = generator.getChildKeyPair(key, 0, false);
        childKey = generator.getChildKeyPair(childKey, 0, false);

        Assert.assertArrayEquals(childPublicKey.getKeyData(), childKey.getPublicKey().getKeyData());


    }
    private static byte[] sign(EdDSAPrivateKey sk, byte[] message) {
        try {
            byte[] sig;

            EdDSAEngine engine = new EdDSAEngine();
            engine.initSign(sk);
            sig = engine.signOneShot(message);

            return sig;
        } catch (SignatureException | InvalidKeyException e) {
            throw new CryptoException(e);
        }
    }

    private static boolean verify(byte[] message, byte[] signature, EdDSAPublicKey pubKey) {
        if (message != null && signature != null) {
            try {

                EdDSAEngine engine = new EdDSAEngine();
                engine.initVerify(pubKey);
                return engine.verifyOneShot(message, signature);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }

        return false;
    }
}
