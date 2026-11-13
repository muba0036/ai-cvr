package dk.skole.ansoegning.company;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Adgang til databasen. Vi skriver kun interfacet - Spring Data laver selv koden bag.
 * JpaRepository giver gratis metoder som findById(...), save(...), findAll() og delete(...).
 * <CompanyCacheEntry, String> = tabellens klasse og typen på primærnøglen.
 */
public interface CompanyCacheRepository extends JpaRepository<CompanyCacheEntry, String> {
}
