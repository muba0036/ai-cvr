package dk.skole.ansoegning.ai;

import dk.skole.ansoegning.company.CompanyInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PromptBuilderTest {

    @Test
    void includesCompanyAndApplicantDataInTags() {
        var info = new CompanyInfo("12345678", "Test ApS", "Odense", "Softwareudvikling", "ApS", "10-19", null);

        String prompt = PromptBuilder.build(info, ApplicationType.PRAKTIK, "Datamatiker, 3. semester", "Praktik i foråret");

        assertTrue(prompt.contains("<virksomhed>"));
        assertTrue(prompt.contains("Navn: Test ApS"));
        assertTrue(prompt.contains("Stiftet: (ukendt)"));
        assertTrue(prompt.contains("Baggrund: Datamatiker, 3. semester"));
        assertTrue(prompt.contains(ApplicationType.PRAKTIK.getLabel()));
    }
}
