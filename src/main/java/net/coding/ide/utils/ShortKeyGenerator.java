/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;

/**
 * Created by phy on 2014/12/3.
 */
@Component
public class ShortKeyGenerator implements RandomGenerator {

    private String[] chars = new String[]{"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k",
            "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"
    };
    private int[] primes = new int[]{2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59,
            61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151,
            157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 223, 227, 229, 233, 239, 241, 251,
            257, 263, 269, 271, 277, 281, 283, 293, 307, 311, 313, 317, 331, 337, 347, 349, 353, 359,
            367, 373, 379, 383, 389, 397, 401, 409, 419, 421, 431, 433, 439, 443, 449, 457, 461, 463,
            467, 479, 487, 491, 499, 503, 509, 521, 523, 541, 547, 557, 563, 569, 571, 577, 587, 593,
            599, 601, 607, 613, 617, 619, 631, 641, 643, 647, 653, 659, 661, 673, 677, 683, 691, 701,
            709, 719, 727, 733, 739, 743, 751, 757, 761, 769, 773, 787, 797, 809, 811, 821, 823, 827,
            829, 839, 853, 857, 859, 863, 877, 881, 883, 887, 907, 911, 919, 929, 937, 941, 947, 953,
            967, 971, 977, 983, 991, 997};

    public String generate(Object... args) {
        int len = args.length;

        long[] seeds = new long[len];

        for (int i = 0; i < len; i++) {
            Object arg = args[i];
            if (arg instanceof Number) {
                seeds[i] = ((Number) arg).longValue();
            } else {
                seeds[i] = arg.hashCode();
            }
        }

        long sum = 0;
        for (int i = 0; i < args.length; i++) {
            sum += seeds[i] * prime();
        }
        String salt = String.valueOf(UUID.randomUUID());
        String source = String.valueOf(sum) + salt;
        String encryptResult = DigestUtils.md5Hex(source);
        String subString = encryptResult.substring(0, 12);
        Long hexLong = Long.parseLong(subString, 16);

        String result = "";
        for (int i = 0; i < 6; i++) {
            // long index = 0x00000033 & hexLong;
            long index = hexLong % 26;
            result += chars[(int) index];
            hexLong = hexLong >> 8;
        }
        return result;
    }

    private int prime() {
        Random random = new Random();
        int idx = random.nextInt(primes.length);
        return primes[idx];
    }

}
