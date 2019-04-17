/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.bip39;

import org.semux.core.bip32.crypto.Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.text.Normalizer;
import java.util.BitSet;

import static org.semux.core.bip32.crypto.BitSetUtil.*;

/**
 * Generate and Process Mnemonic codes
 */
public class MnemonicGenerator {

    private static final Logger logger = LoggerFactory.getLogger(MnemonicGenerator.class);

    public static final String SPACE_JP = "\u3000";

    private SecureRandom secureRandom = new SecureRandom();

    public byte[] getSeedFromWordlist(String words, String password, Language language) {
        if (password == null) {
            password = "";
        }

        // check the words are valid (will throw exception if invalid)
        getEntropy(words, language);

        if (password == null) {
            password = "";
        }

        password = Normalizer.normalize(password, Normalizer.Form.NFKD);
        words = Normalizer.normalize(words, Normalizer.Form.NFKD);

        String salt = "mnemonic" + password;
        return pbkdf2HmacSha512(words.trim().toCharArray(), salt.getBytes(Charset.forName("UTF-8")), 2048, 512);
    }

    protected byte[] getEntropy(String words, Language language) {

        Dictionary dictionary;

        try {
            dictionary = new Dictionary(language);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unknown dictionary");
        }

        String[] wordsList;
        if (language != Language.japanese) {
            words = words.replaceAll(SPACE_JP, " ");
            wordsList = words.split(" ");
        } else {
            wordsList = words.split("" + SPACE_JP);
        }

        // validate that things look alright
        if (wordsList.length < 12) {
            throw new IllegalArgumentException("Must be at least 12 words");
        }
        if (wordsList.length > 24) {
            throw new IllegalArgumentException("Must be less than 24 words");
        }

        BitSet bitSet = new BitSet();
        for (int i = 0; i < wordsList.length; i++) {
            String word = wordsList[i];
            int code = dictionary.indexOf(word.trim());
            bitSet = addCode(bitSet, code);
            if (code < 0) {
                throw new IllegalArgumentException("Unknown word: " + word);

            }
        }
        int numBits = wordsList.length * 11;

        int csBits = numBits % 8;
        // handle 8 bit cs, which is max
        if (csBits == 0) {
            csBits = 8;
        }
        int entBits = numBits - csBits;

        byte[] entropy = createBytes(bitSet.get(0, entBits), entBits / 8);
        BitSet checksum = bitSet.get(entBits, numBits);

        // todo - stopping point, checksum still not correct! (Even for valid values)
        byte[] calculatedChecksum = Hash.sha256(entropy);
        BitSet checksumBs = createBitset(calculatedChecksum).get(0, csBits);

        if (!checksumBs.equals(checksum)) {
            // throw new IllegalArgumentException("Checksum does not match, invalid words");
        }

        return entropy;
    }

    private byte[] pbkdf2HmacSha512(final char[] password, final byte[] salt, final int iterations,
            final int keyLength) {

        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            SecretKey key = skf.generateSecret(spec);
            byte[] res = key.getEncoded();
            return res;

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public String getWordlist(int entropyLength, Language language) {
        byte[] entropy = secureRandom.generateSeed(entropyLength / 8);
        return getWordlist(entropy, language);
    }

    public String getWordlist(byte[] entropy, Language language) {

        int entropyLength = entropy.length * 8;
        Dictionary dictionary;
        try {
            dictionary = new Dictionary(language);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unknown dictionary");
        }
        if (entropyLength < 128) {
            throw new IllegalArgumentException("Entropy must be over 128");
        }
        if (entropyLength > 256) {
            throw new IllegalArgumentException("Entropy must be less than 256");
        }
        if (entropyLength % 32 != 0) {
            throw new IllegalArgumentException("Entropy must be a multiple of 32");
        }
        int checksumLength = entropyLength / 32;
        byte[] hash = Hash.sha256(entropy);

        BitSet hashBitset = createBitset(hash);
        BitSet bitSet = createBitset(entropy);

        BitSet checksum = hashBitset.get(0, checksumLength);
        bitSet = append(checksum, bitSet, entropyLength);

        StringBuilder ret = new StringBuilder();

        int numWords = (entropyLength + checksumLength) / 11;
        for (int i = 0; i < numWords; i++) {
            BitSet range = bitSet.get(i * 11, (i + 1) * 11);
            int wordIdx = 0;
            if (!range.isEmpty()) {
                wordIdx = getInt(range);
            }
            String word = dictionary.getWord(wordIdx);
            if (i > 0) {
                if (language == Language.japanese) {
                    ret.append(SPACE_JP);
                } else {
                    ret.append(" ");
                }

            }
            ret.append(word);

        }

        return ret.toString();
    }
}
