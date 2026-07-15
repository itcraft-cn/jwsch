package cn.itcraft.jwsch.common.util;

import java.util.Objects;

public final class ValidateUtils {
    
    private ValidateUtils() {
    }
    
    public static <T> T notNull(T obj, String message) {
        return Objects.requireNonNull(obj, message);
    }
    
    public static void checkArgument(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
    
    public static void checkState(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
    
    public static void positive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
    
    public static void notNegative(int value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
    }
    
    public static void inRange(int value, int min, int max, String message) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(message);
        }
    }
}