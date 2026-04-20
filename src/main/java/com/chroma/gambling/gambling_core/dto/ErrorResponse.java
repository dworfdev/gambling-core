package com.chroma.gambling.gambling_core.dto;

public class ErrorResponse
{
    private String message;
    private long timestamp;

    public ErrorResponse(String message)
    {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
}