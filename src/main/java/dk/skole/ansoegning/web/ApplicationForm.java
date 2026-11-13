package dk.skole.ansoegning.web;

import dk.skole.ansoegning.ai.ApplicationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Formulardata fra forsiden. Spring fylder felterne ud automatisk, når formularen sendes.
 * Gemmes ikke - bruges kun til at lave udkastet.
 *
 * Annotationerne (@NotBlank, @Size ...) er valideringsregler. Brydes en regel,
 * vises beskeden under feltet på siden.
 */
public class ApplicationForm {

    @NotBlank(message = "Indtast et virksomhedsnavn") // Må ikke være tomt
    @Size(max = 120)                                    // Maks. 120 tegn
    private String companyName;

    @NotNull
    private ApplicationType type = ApplicationType.PRAKTIK; // Standardvalg i dropdown-menuen

    @NotBlank(message = "Beskriv kort din baggrund")
    @Size(max = 2000, message = "Hold baggrunden under 2000 tegn")
    private String background;

    @NotBlank(message = "Beskriv hvad du søger")
    @Size(max = 1000, message = "Hold formålet under 1000 tegn")
    private String purpose;

    // Getters og setters - Spring og Thymeleaf bruger dem til at læse/skrive felterne
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public ApplicationType getType() { return type; }
    public void setType(ApplicationType type) { this.type = type; }
    public String getBackground() { return background; }
    public void setBackground(String background) { this.background = background; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}
