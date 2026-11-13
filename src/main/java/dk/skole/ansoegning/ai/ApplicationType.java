package dk.skole.ansoegning.ai;

/**
 * De typer udkast brugeren kan vælge i dropdown-menuen.
 * Teksten i parentesen er det, der vises på siden og sendes til AI'en.
 */
public enum ApplicationType {
    PRAKTIK("Praktikforespørgsel"),
    JOB("Jobansøgning"),
    UOPFORDRET("Uopfordret ansøgning");

    private final String label;

    ApplicationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
