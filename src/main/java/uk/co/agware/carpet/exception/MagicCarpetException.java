package uk.co.agware.carpet.exception;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 7/05/2016.
 */
public class MagicCarpetException extends Exception {

    public MagicCarpetException() {
    }

    public MagicCarpetException(String message) {
        super(message);
    }

    public MagicCarpetException(String message, Throwable cause) {
        super(message, cause);
    }

    public MagicCarpetException(Throwable cause) {
        super(cause);
    }

    public MagicCarpetException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
