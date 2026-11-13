package dk.skole.ansoegning.company;

/** Fejl ved opslag, med en besked der kan vises direkte til brugeren. */
public class CompanyLookupException extends RuntimeException {
    public CompanyLookupException(String userMessage) {
        super(userMessage);
    }

    public CompanyLookupException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
