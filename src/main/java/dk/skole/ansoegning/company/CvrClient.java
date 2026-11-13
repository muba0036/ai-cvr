package dk.skole.ansoegning.company;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Kalder cvrapi.dk. Mapper KUN de felter vi har brug for - ejere,
 * telefonnummer og e-mail bliver aldrig læst ind i appen.
 */
@Component // Spring opretter klassen automatisk, så den kan bruges i CompanyService
public class CvrClient {

    // RestClient er Springs værktøj til at lave HTTP/REST-kald
    private final RestClient restClient;

    // Sætter klienten op med API'ets adresse og en User-Agent (cvrapi.dk kræver, at man fortæller hvem man er)
    public CvrClient(@Value("${cvr.base-url}") String baseUrl,
                     @Value("${cvr.user-agent}") String userAgent) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", userAgent)
                .build();
    }

    /** Søger på et virksomhedsnavn og returnerer de offentlige oplysninger. */
    public CompanyInfo search(String companyName) {
        CvrResponse response;
        try {
            // Laver kaldet: GET https://cvrapi.dk/api?search=<navn>&country=dk
            // og laver JSON-svaret om til et CvrResponse-objekt
            response = restClient.get()
                    .uri(b -> b.queryParam("search", companyName).queryParam("country", "dk").build())
                    .retrieve()
                    .body(CvrResponse.class);
        } catch (HttpClientErrorException e) {
            // API'et svarede med en fejlkode (4xx) - oversæt til en dansk besked
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new CompanyLookupException("CVR-API'ets kvote er brugt op. Prøv igen senere.", e);
            }
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new CompanyLookupException("Ingen virksomhed fundet med navnet \"" + companyName + "\".", e);
            }
            throw new CompanyLookupException("CVR-opslaget fejlede (" + e.getStatusCode().value() + ").", e);
        } catch (RestClientException e) {
            // Ingen forbindelse overhovedet (fx intet internet)
            throw new CompanyLookupException("Kunne ikke kontakte CVR-API'et. Tjek din internetforbindelse.", e);
        }

        // cvrapi.dk kan også svare "200 OK" men med et "error"-felt i JSON - det tjekker vi her
        if (response == null) {
            throw new CompanyLookupException("Tomt svar fra CVR-API'et.");
        }
        if (response.error() != null) {
            if ("QUOTA_EXCEEDED".equals(response.error())) {
                throw new CompanyLookupException("CVR-API'ets kvote er brugt op. Prøv igen senere.");
            }
            if ("NOT_FOUND".equals(response.error())) {
                throw new CompanyLookupException("Ingen virksomhed fundet med navnet \"" + companyName + "\".");
            }
            throw new CompanyLookupException("CVR-API'et svarede med fejl: " + response.error());
        }

        // Alt gik godt: flyt de felter vi bruger over i vores egen CompanyInfo
        return new CompanyInfo(
                response.vat() == null ? null : String.valueOf(response.vat()),
                response.name(),
                response.city(),
                response.industrydesc(),
                response.companydesc(),
                response.employees(),
                response.startdate());
    }

    /**
     * Svaret fra cvrapi.dk. Navnene matcher JSON-felterne i svaret.
     * ignoreUnknown = true betyder, at alle andre felter (ejere, telefon, e-mail ...) bliver smidt væk.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record CvrResponse(
            Long vat,             // CVR-nummer
            String name,          // Virksomhedens navn
            String city,          // By
            String industrydesc,  // Branche
            String companydesc,   // Virksomhedsform (ApS, A/S ...)
            String employees,     // Antal ansatte (interval)
            String startdate,     // Stiftelsesdato
            String error) {       // Fejlkode, hvis noget gik galt
    }
}
