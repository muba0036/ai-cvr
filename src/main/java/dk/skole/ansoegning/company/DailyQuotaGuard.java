package dk.skole.ansoegning.company;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Tæller hvor mange kald vi har lavet til CVR-API'et i dag og stopper
 * os selv FØR vi rammer API'ets egen grænse. Vi respekterer grænsen
 * i stedet for at omgå den.
 */
@Component // Spring opretter ÉN instans, som deles af hele appen - derfor tæller den på tværs af brugere
public class DailyQuotaGuard {

    private final int dailyLimit; // Maks. kald pr. døgn (cvr.daily-limit i application.properties)
    private final Clock clock;    // Uret - kan skiftes ud i tests
    private LocalDate day;        // Hvilken dag vi tæller for
    private int used;             // Hvor mange kald vi har brugt i dag

    // Bruges af Spring. Grænsen læses fra application.properties
    @Autowired
    public DailyQuotaGuard(@Value("${cvr.daily-limit}") int dailyLimit) {
        this(dailyLimit, Clock.systemDefaultZone());
    }

    // Bruges i tests med et fast ur
    DailyQuotaGuard(int dailyLimit, Clock clock) {
        this.dailyLimit = dailyLimit;
        this.clock = clock;
        this.day = LocalDate.now(clock);
    }

    /**
     * Kaldes lige før hvert CVR-kald. Kaster en fejl, hvis dagens grænse er nået.
     * "synchronized" sikrer, at to brugere ikke tæller forkert, hvis de søger samtidig.
     */
    public synchronized void acquire() {
        // Ny dag? Så nulstil tælleren
        LocalDate today = LocalDate.now(clock);
        if (!today.equals(day)) {
            day = today;
            used = 0;
        }
        // Grænsen nået? Stop og giv brugeren en forklaring
        if (used >= dailyLimit) {
            throw new CompanyLookupException(
                    "Dagens grænse for nye virksomhedsopslag (" + dailyLimit + ") er nået. "
                            + "Tidligere søgte virksomheder kan stadig bruges.");
        }
        // Ellers: tæl ét kald op
        used++;
    }

    /** Hvor mange kald der er tilbage i dag. */
    public synchronized int remaining() {
        return LocalDate.now(clock).equals(day) ? dailyLimit - used : dailyLimit;
    }
}
