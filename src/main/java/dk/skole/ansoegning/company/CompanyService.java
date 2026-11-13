package dk.skole.ansoegning.company;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Slår en virksomhed op. Bruger cachen hvis opslaget er nyt nok,
 * ellers kaldes CVR-API'et (inden for vores daglige kvote).
 * Kun virksomheder brugeren faktisk søger på bliver gemt.
 */
@Service // Fortæller Spring, at dette er en "service" (forretningslogik), som Spring selv opretter
public class CompanyService {

    // De ting servicen har brug for. Spring "sprøjter" dem ind via konstruktøren (dependency injection)
    private final CvrClient cvrClient;          // Laver det rigtige kald til cvrapi.dk
    private final CompanyCacheRepository cache;  // Læser/gemmer i databasen
    private final DailyQuotaGuard quota;         // Holder øje med dagens antal API-kald
    private final Duration ttl;                  // Hvor længe et opslag er gyldigt (fx 7 dage)
    private final Clock clock;                   // Uret - kan skiftes ud i tests

    // Denne konstruktør bruger Spring. ttl læses fra application.properties (cvr.cache-ttl)
    @Autowired
    public CompanyService(CvrClient cvrClient,
                          CompanyCacheRepository cache,
                          DailyQuotaGuard quota,
                          @Value("${cvr.cache-ttl}") Duration ttl) {
        this(cvrClient, cache, quota, ttl, Clock.systemUTC());
    }

    // Denne konstruktør bruges i testene, så vi kan "fryse" tiden
    CompanyService(CvrClient cvrClient, CompanyCacheRepository cache, DailyQuotaGuard quota,
                   Duration ttl, Clock clock) {
        this.cvrClient = cvrClient;
        this.cache = cache;
        this.quota = quota;
        this.ttl = ttl;
        this.clock = clock;
    }

    /** Hovedmetoden: find en virksomhed ud fra navnet. */
    public LookupResult lookup(String companyName) {
        // 1) Tjek at brugeren faktisk har skrevet noget
        if (companyName == null || companyName.isBlank()) {
            throw new CompanyLookupException("Indtast et virksomhedsnavn.");
        }
        // 2) Lav en ens nøgle, så "Netcompany", " netcompany " osv. rammer samme cache-række
        String key = normalize(companyName);
        Instant now = clock.instant();

        // 3) Findes virksomheden i cachen, og er den yngre end ttl? Så brug den - intet API-kald
        var cached = cache.findById(key);
        if (cached.isPresent() && cached.get().getFetchedAt().plus(ttl).isAfter(now)) {
            return new LookupResult(cached.get().toCompanyInfo(), true);
        }

        // 4) Ellers: tjek dagens kvote, kald CVR-API'et og gem svaret i cachen
        quota.acquire();
        CompanyInfo info = cvrClient.search(companyName.trim());
        cache.save(new CompanyCacheEntry(key, info, now));
        return new LookupResult(info, false);
    }

    /** Gør navnet ens: fjern mellemrum i enderne, små bogstaver, kun ét mellemrum mellem ord. */
    static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    /** Resultatet: virksomhedens data + om det kom fra cachen (vises som "fra cache" på siden). */
    public record LookupResult(CompanyInfo company, boolean fromCache) {
    }
}
