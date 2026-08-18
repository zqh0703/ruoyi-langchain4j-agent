package org.dromara.agent.action;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("local")
class TemporaryPasswordGeneratorTest {

    @Test
    void shouldGenerateStrongNonRepeatingTemporaryPasswords() {
        TemporaryPasswordGenerator generator = new TemporaryPasswordGenerator();
        Set<String> generated = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            String password = generator.generate();
            assertEquals(16, password.length());
            assertTrue(password.chars().anyMatch(Character::isUpperCase));
            assertTrue(password.chars().anyMatch(Character::isLowerCase));
            assertTrue(password.chars().anyMatch(Character::isDigit));
            assertTrue(password.chars().anyMatch(value -> "!@#$%".indexOf(value) >= 0));
            generated.add(password);
        }

        assertEquals(100, generated.size());
    }
}
