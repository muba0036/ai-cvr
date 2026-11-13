package dk.skole.ansoegning.company;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompanyServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");
    private static final CompanyInfo INFO =
            new CompanyInfo("12345678", "Test ApS", "Odense", "Softwareudvikling", "ApS", "10-19", "01/01 - 2015");

    private CvrClient cvrClient;
    private CompanyCacheRepository repo;
    private CompanyService service;

    @BeforeEach
    void setUp() {
        cvrClient = mock(CvrClient.class);
        repo = mock(CompanyCacheRepository.class);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new CompanyService(cvrClient, repo, new DailyQuotaGuard(2, clock), Duration.ofDays(7), clock);
    }

    @Test
    void usesFreshCacheWithoutCallingApi() {
        when(repo.findById("test aps")).thenReturn(Optional.of(
                new CompanyCacheEntry("test aps", INFO, NOW.minus(Duration.ofDays(1)))));

        var result = service.lookup("  Test   ApS ");

        assertTrue(result.fromCache());
        assertEquals("Test ApS", result.company().name());
        verifyNoInteractions(cvrClient);
    }

    @Test
    void callsApiWhenCacheIsExpiredAndSavesResult() {
        when(repo.findById("test aps")).thenReturn(Optional.of(
                new CompanyCacheEntry("test aps", INFO, NOW.minus(Duration.ofDays(8)))));
        when(cvrClient.search("Test ApS")).thenReturn(INFO);

        var result = service.lookup("Test ApS");

        assertFalse(result.fromCache());
        verify(cvrClient).search("Test ApS");
        verify(repo).save(any(CompanyCacheEntry.class));
    }

    @Test
    void stopsAtDailyLimit() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        when(cvrClient.search(any())).thenReturn(INFO);

        service.lookup("a");
        service.lookup("b");
        var ex = assertThrows(CompanyLookupException.class, () -> service.lookup("c"));

        assertTrue(ex.getMessage().contains("grænse"));
        verify(cvrClient, times(2)).search(any());
    }

    @Test
    void rejectsBlankName() {
        assertThrows(CompanyLookupException.class, () -> service.lookup("   "));
    }
}
