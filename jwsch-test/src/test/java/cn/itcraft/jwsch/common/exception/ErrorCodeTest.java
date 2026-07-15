package cn.itcraft.jwsch.common.exception;

import org.junit.Test;

import static org.junit.Assert.*;

public class ErrorCodeTest {
    
    @Test
    public void testSuccess() {
        ErrorCode errorCode = ErrorCode.SUCCESS;
        assertEquals(0, errorCode.getCode());
        assertEquals("Success", errorCode.getDesc());
    }
    
    @Test
    public void testInvalidMagic() {
        ErrorCode errorCode = ErrorCode.INVALID_MAGIC;
        assertEquals(1, errorCode.getCode());
        assertEquals("Invalid magic", errorCode.getDesc());
    }
    
    @Test
    public void testConnectionClosed() {
        ErrorCode errorCode = ErrorCode.CONNECTION_CLOSED;
        assertEquals(1001, errorCode.getCode());
        assertEquals("Connection closed", errorCode.getDesc());
    }
    
    @Test
    public void testServiceNotFound() {
        ErrorCode errorCode = ErrorCode.SERVICE_NOT_FOUND;
        assertEquals(2001, errorCode.getCode());
        assertEquals("Service not found", errorCode.getDesc());
    }
    
    @Test
    public void testInternalError() {
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        assertEquals(9001, errorCode.getCode());
        assertEquals("Internal error", errorCode.getDesc());
    }
    
    @Test
    public void testFromCode_validCode() {
        ErrorCode errorCode = ErrorCode.fromCode((short) 0);
        assertEquals(ErrorCode.SUCCESS, errorCode);
        
        errorCode = ErrorCode.fromCode((short) 1);
        assertEquals(ErrorCode.INVALID_MAGIC, errorCode);
        
        errorCode = ErrorCode.fromCode((short) 2001);
        assertEquals(ErrorCode.SERVICE_NOT_FOUND, errorCode);
    }
    
    @Test
    public void testFromCode_invalidCode() {
        ErrorCode errorCode = ErrorCode.fromCode((short) 9999);
        assertNull(errorCode);
    }
}