package cn.itcraft.jwsch.common.util;

import java.util.Objects;

/**
 * Validation utility methods.
 * 
 * <p>Provides common validation patterns with consistent error messages.
 */
public final class ValidateUtils {
    
    private ValidateUtils() {
    }
    
    /**
     * Checks that an object is not null.
     * 
     * @param obj object to check
     * @param message error message for NullPointerException
     * @return the object if not null
     * @throws NullPointerException if obj is null
     */
    public static <T> T notNull(T obj, String message) {
        return Objects.requireNonNull(obj, message);
    }
    
    /**
     * Checks a boolean argument condition.
     * 
     * @param condition condition that must be true
     * @param message error message for IllegalArgumentException
     * @throws IllegalArgumentException if condition is false
     */
    public static void checkArgument(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * Checks a boolean state condition.
     * 
     * @param condition condition that must be true
     * @param message error message for IllegalStateException
     * @throws IllegalStateException if condition is false
     */
    public static void checkState(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
    
    /**
     * Checks that a value is positive (>0).
     * 
     * @param value value to check
     * @param message error message for IllegalArgumentException
     * @throws IllegalArgumentException if value <= 0
     */
    public static void positive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * Checks that a value is not negative (>=0).
     * 
     * @param value value to check
     * @param message error message for IllegalArgumentException
     * @throws IllegalArgumentException if value < 0
     */
    public static void notNegative(int value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * Checks that a value is within inclusive range [min, max].
     * 
     * @param value value to check
     * @param min minimum allowed value (inclusive)
     * @param max maximum allowed value (inclusive)
     * @param message error message for IllegalArgumentException
     * @throws IllegalArgumentException if value < min or value > max
     */
    public static void inRange(int value, int min, int max, String message) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(message);
        }
    }
}