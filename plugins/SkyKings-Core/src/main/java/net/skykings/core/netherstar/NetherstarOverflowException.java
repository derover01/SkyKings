package net.skykings.core.netherstar;

/** Wird geworfen, wenn eine Netherstar-Einzahlung den gueltigen {@code long}-Wertebereich ueberschreiten wuerde. */
public class NetherstarOverflowException extends RuntimeException {

    public NetherstarOverflowException(String message) {
        super(message);
    }

    public NetherstarOverflowException(String message, Throwable cause) {
        super(message, cause);
    }
}
