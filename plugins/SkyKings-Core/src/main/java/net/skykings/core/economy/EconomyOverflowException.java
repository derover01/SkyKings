package net.skykings.core.economy;

/** Wird geworfen, wenn eine Einzahlung den gueltigen {@code long}-Wertebereich ueberschreiten wuerde. */
public class EconomyOverflowException extends RuntimeException {

    public EconomyOverflowException(String message) {
        super(message);
    }

    public EconomyOverflowException(String message, Throwable cause) {
        super(message, cause);
    }
}
