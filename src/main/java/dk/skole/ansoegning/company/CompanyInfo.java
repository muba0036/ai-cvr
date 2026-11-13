package dk.skole.ansoegning.company;

/**
 * De offentlige virksomhedsoplysninger appen bruger.
 * Bevidst UDEN ejere, telefon og e-mail (dataminimering / GDPR).
 */
public record CompanyInfo(
        String cvr,
        String name,
        String city,
        String industry,
        String companyType,
        String employees,
        String startDate) {
}
