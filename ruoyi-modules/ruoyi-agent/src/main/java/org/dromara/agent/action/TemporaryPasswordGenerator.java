package org.dromara.agent.action;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates a 16-character password that is never persisted in plaintext.
 */
@Component
public class TemporaryPasswordGenerator {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%";
    private static final String ALL = UPPER + LOWER + DIGITS + SYMBOLS;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        List<Character> value = new ArrayList<>(16);
        value.add(pick(UPPER));
        value.add(pick(LOWER));
        value.add(pick(DIGITS));
        value.add(pick(SYMBOLS));
        while (value.size() < 16) {
            value.add(pick(ALL));
        }
        Collections.shuffle(value, random);
        StringBuilder password = new StringBuilder(value.size());
        value.forEach(password::append);
        return password.toString();
    }

    private char pick(String source) {
        return source.charAt(random.nextInt(source.length()));
    }
}
