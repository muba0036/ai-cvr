package dk.skole.ansoegning.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dk.skole.ansoegning.company.CompanyInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Sender virksomhedsdata + brugerens baggrund til OpenAI (Chat Completions REST-API)
 * og får et udkast tilbage.
 */
@Service
public class AiDraftService {

    private final RestClient restClient; // Springs værktøj til HTTP/REST-kald
    private final String model;          // Hvilken AI-model (ai.model i application.properties)
    private final String apiKey;         // Nøglen fra miljøvariablen OPENAI_API_KEY

    // Spring indsætter værdierne. "${OPENAI_API_KEY:}" = læs miljøvariablen, eller tom tekst hvis den mangler
    public AiDraftService(@Value("${ai.base-url}") String baseUrl,
                          @Value("${ai.model}") String model,
                          @Value("${OPENAI_API_KEY:}") String apiKey) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.model = model;
        this.apiKey = apiKey;
    }

    /** Beder AI'en om at skrive et udkast og returnerer teksten. */
    public String generateDraft(CompanyInfo company, ApplicationType type,
                                String background, String purpose) {
        // 1) Uden nøgle kan vi ikke kalde OpenAI - giv en tydelig besked
        if (apiKey == null || apiKey.isBlank()) {
            throw new DraftGenerationException("Miljøvariablen OPENAI_API_KEY er ikke sat. Se README.");
        }

        // 2) Byg forespørgslen: en "system"-besked med reglerne + en "user"-besked med data
        var request = new ChatRequest(model, List.of(
                new ChatMessage("system", PromptBuilder.SYSTEM_PROMPT),
                new ChatMessage("user", PromptBuilder.build(company, type, background, purpose))));

        ChatResponse response;
        try {
            // 3) Send: POST https://api.openai.com/v1/chat/completions
            //    Nøglen sendes i "Authorization"-headeren. Objektet laves automatisk om til JSON
            response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);
        } catch (HttpClientErrorException e) {
            // 4) OpenAI svarede med en fejlkode - oversæt til danske beskeder
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {             // 401: forkert nøgle
                throw new DraftGenerationException("AI-nøglen er ugyldig. Tjek OPENAI_API_KEY.", e);
            }
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {        // 429: for mange kald / ingen penge
                if (e.getResponseBodyAsString().contains("insufficient_quota")) {
                    throw new DraftGenerationException("Der er ikke flere credits på OpenAI-kontoen.", e);
                }
                throw new DraftGenerationException("AI-tjenesten er overbelastet lige nu. Prøv igen om lidt.", e);
            }
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {                // 404: modellen findes ikke
                throw new DraftGenerationException("AI-modellen \"" + model + "\" findes ikke. Tjek ai.model.", e);
            }
            throw new DraftGenerationException("AI-tjenesten svarede med en fejl (" + e.getStatusCode().value() + ").", e);
        } catch (RestClientException e) {
            // Ingen forbindelse til OpenAI (fx intet internet)
            throw new DraftGenerationException("Kunne ikke kontakte AI-tjenesten.", e);
        }

        // 5) Pak svaret ud: teksten ligger i choices[0].message.content
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new DraftGenerationException("AI'en returnerede et tomt svar. Prøv igen.");
        }
        ResponseMessage message = response.choices().getFirst().message();
        // Har modellen afvist opgaven, står begrundelsen i "refusal"
        if (message.refusal() != null && !message.refusal().isBlank()) {
            throw new DraftGenerationException("AI'en ville ikke skrive dette udkast. Prøv at omformulere din tekst.");
        }
        if (message.content() == null || message.content().isBlank()) {
            throw new DraftGenerationException("AI'en returnerede et tomt svar. Prøv igen.");
        }
        return message.content().strip();
    }

    // ----- Records der matcher OpenAI's JSON-format (Jackson laver JSON <-> Java automatisk) -----

    // Det vi sender: {"model": "...", "messages": [{"role": "...", "content": "..."}]}
    record ChatRequest(String model, List<ChatMessage> messages) {
    }

    record ChatMessage(String role, String content) {
    }

    // Det vi får tilbage. ignoreUnknown = vi ignorerer de felter, vi ikke bruger
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(ResponseMessage message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResponseMessage(String content, String refusal) {
    }
}
