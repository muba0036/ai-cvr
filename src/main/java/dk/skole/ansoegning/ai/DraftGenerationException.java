package dk.skole.ansoegning.ai;

/** Fejl fra AI-delen. Beskeden vises direkte til brugeren i den røde fejlboks. */
public class DraftGenerationException extends RuntimeException {
    public DraftGenerationException(String userMessage) {
        super(userMessage);
    }

    public DraftGenerationException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
