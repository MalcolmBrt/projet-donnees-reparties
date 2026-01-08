package common;

public class MoveException extends Exception {
    public MoveException(String message) { super(message); }
    public MoveException(String message, Throwable cause) { super(message, cause); }
}