package dk.skole.ansoegning.company;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;

/**
 * Et cachet opslag = én række i databasetabellen COMPANY_CACHE_ENTRY.
 * Hvert felt herunder bliver til en kolonne i tabellen.
 */
@Entity // Fortæller JPA/Hibernate, at klassen skal gemmes i databasen (tabellen laves automatisk)
public class CompanyCacheEntry {

    @Id // Primærnøgle: det normaliserede søgenavn, fx "netcompany"
    private String searchKey;
    private String cvr;
    private String name;
    private String city;
    private String industry;
    private String companyType;
    private String employees;
    private String startDate;
    private Instant fetchedAt; // Hvornår vi hentede data fra API'et - bruges til at se om cachen er for gammel

    // Tom konstruktør, som JPA kræver for at kunne læse rækker fra databasen
    protected CompanyCacheEntry() {
    }

    // Laver en ny række ud fra et API-svar
    public CompanyCacheEntry(String searchKey, CompanyInfo info, Instant fetchedAt) {
        this.searchKey = searchKey;
        this.cvr = info.cvr();
        this.name = info.name();
        this.city = info.city();
        this.industry = info.industry();
        this.companyType = info.companyType();
        this.employees = info.employees();
        this.startDate = info.startDate();
        this.fetchedAt = fetchedAt;
    }

    // Laver rækken om til et CompanyInfo-objekt, som resten af appen bruger
    public CompanyInfo toCompanyInfo() {
        return new CompanyInfo(cvr, name, city, industry, companyType, employees, startDate);
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }
}
