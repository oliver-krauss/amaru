/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

public class HashUtil {


    private static Base64.Decoder decoder = Base64.getDecoder();

    private static Base64.Encoder encoder = Base64.getEncoder();

    /**
     * Creates a hash of a byte array and then encodes it into a string
     *
     * @param value to be hashed and encoded
     * @return Base64 encoded string
     */
    public static String hashAndEncode(byte[] value) {
        return encoder.encodeToString(hash(value));
    }

    public static String hashAndEncode(byte[][] hashes) {
        // turn the hash-map into a sequence
        AtomicInteger length = new AtomicInteger();
        Arrays.stream(hashes).forEach(x -> length.addAndGet(x.length));
        byte[] hash = new byte[length.get()];
        int i = 0;
        for (byte[] bytes : hashes) {
            for (byte aByte : bytes) {
                hash[i] = aByte;
                i++;
            }
        }
        return hashAndEncode(hash);

    }

    /**
     * Encodes a byte array into a string
     *
     * @param value to be encoded
     * @return Base64 encoded version of the byte array
     */
    public static String encode(byte[] value) {
        return encoder.encodeToString(value);
    }

    /**
     * Decodes a hash-string into a byte array
     *
     * @param value to be decoded
     * @return byte array of the decoded hash
     */
    public static byte[] decodeHash(String value) {
        return decoder.decode(value);
    }

    /**
     * Creates a hash over the byte array
     *
     * @param value to be hashed
     * @return hash value of the byte array
     */
    public static byte[] hash(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-512").digest(value);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }


}
