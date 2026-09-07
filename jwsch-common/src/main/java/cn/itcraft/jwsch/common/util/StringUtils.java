package cn.itcraft.jwsch.common.util;

/**
 * String utility methods.
 * 
 * <p>Common string operations used throughout jwsch.
 */
public final class StringUtils {
    
    private StringUtils() {
    }
    
    /**
     * Checks if a string is null or empty.
     * 
     * @param str the string to check
     * @return true if str is null or empty, false otherwise
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
    
    /**
     * Checks if a string is not null and not empty.
     * 
     * @param str the string to check
     * @return true if str is not null and not empty, false otherwise
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * Returns the string itself if not empty, otherwise returns default value.
     * 
     * @param str the string to check
     * @param defaultValue default value to return if str is empty
     * @return str if not empty, defaultValue otherwise
     */
    public static String defaultIfEmpty(String str, String defaultValue) {
        return isEmpty(str) ? defaultValue : str;
    }
}