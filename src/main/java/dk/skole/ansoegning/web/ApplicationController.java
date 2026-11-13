package dk.skole.ansoegning.web;

import dk.skole.ansoegning.ai.AiDraftService;
import dk.skole.ansoegning.ai.ApplicationType;
import dk.skole.ansoegning.ai.DraftGenerationException;
import dk.skole.ansoegning.company.CompanyLookupException;
import dk.skole.ansoegning.company.CompanyService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Controlleren tager imod forespørgsler fra browseren og bestemmer, hvilken HTML-side der vises.
 * Den binder "company"-delen og "ai"-delen sammen.
 */
@Controller
public class ApplicationController {

    private final CompanyService companyService; // Finder virksomhedsdata
    private final AiDraftService aiDraftService; // Skriver udkastet med AI

    // Spring giver os de to services automatisk (dependency injection)
    public ApplicationController(CompanyService companyService, AiDraftService aiDraftService) {
        this.companyService = companyService;
        this.aiDraftService = aiDraftService;
    }

    // Gør listen af ansøgningstyper tilgængelig i HTML'en (bruges til dropdown-menuen)
    @ModelAttribute("types")
    public ApplicationType[] types() {
        return ApplicationType.values();
    }

    // GET http://localhost:8081/  -> vis forsiden (templates/index.html) med en tom formular
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("form", new ApplicationForm());
        return "index";
    }

    // POST /generate -> kaldes når brugeren trykker "Lav udkast"
    @PostMapping("/generate")
    public String generate(@Valid @ModelAttribute("form") ApplicationForm form,
                           BindingResult binding, Model model) {
        // 1) Er formularen udfyldt forkert (fx tomme felter)? Vis forsiden igen med fejlbeskeder
        if (binding.hasErrors()) {
            return "index";
        }
        try {
            // 2) Find virksomheden (cache eller CVR-API)
            var lookup = companyService.lookup(form.getCompanyName());
            // 3) Bed AI'en om at skrive udkastet
            String draft = aiDraftService.generateDraft(
                    lookup.company(), form.getType(), form.getBackground(), form.getPurpose());
            // 4) Læg data klar til resultatsiden og vis templates/result.html
            model.addAttribute("company", lookup.company());
            model.addAttribute("fromCache", lookup.fromCache());
            model.addAttribute("draft", draft);
            return "result";
        } catch (CompanyLookupException | DraftGenerationException e) {
            // Gik noget galt? Vis forsiden igen med en rød fejlboks
            model.addAttribute("error", e.getMessage());
            return "index";
        }
    }
}
