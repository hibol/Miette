package com.hibol.miette.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hibol.miette.entity.User;
import com.hibol.miette.repository.UserRepository;
import com.hibol.miette.service.AppSettingService;
import com.hibol.miette.service.IngredientService;
import com.hibol.miette.service.RecipeIndexingService;
import com.hibol.miette.service.UmamiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final RecipeIndexingService indexingService;
    private final IngredientService ingredientService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UmamiService umamiService;
    private final AppSettingService appSettingService;

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("allIngredients", ingredientService.findFlourIngredients());
        model.addAttribute("orphanIngredients", ingredientService.findOrphans());
        model.addAttribute("umamiData", umamiService.fetchStats());
        model.addAttribute("appSettings", appSettingService.findAll());
        return "admin";
    }

    @PostMapping("/admin/ingredients/gluten")
    public String updateGlutenStrengths(@RequestParam Map<String, String> params,
                                        RedirectAttributes redirectAttributes) {
        params.entrySet().stream()
            .filter(e -> e.getKey().startsWith("g__"))
            .forEach(e -> {
                Long id = Long.parseLong(e.getKey().substring(3));
                String raw = e.getValue().trim();
                Double value = raw.isEmpty() ? null : Double.parseDouble(raw);
                ingredientService.updateGlutenStrength(id, value);
            });
        redirectAttributes.addFlashAttribute("message", "Valeurs de gluten mises à jour.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/settings")
    public String updateSettings(@RequestParam Map<String, String> params,
                                 RedirectAttributes redirectAttributes) {
        params.entrySet().stream()
            .filter(e -> e.getKey().startsWith("s__"))
            .forEach(e -> appSettingService.update(e.getKey().substring(3), e.getValue()));
        redirectAttributes.addFlashAttribute("message", "✅ Paramètres mis à jour.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/search/reindex")
    public String reindex(RedirectAttributes redirectAttributes) {
        indexingService.rebuildIndex();
        redirectAttributes.addFlashAttribute("message", "✅ Index de recherche reconstruit !");
        return "redirect:/admin";
    }

    @PostMapping("/admin/ingredients/purge-orphans")
    public String purgeOrphanIngredients(RedirectAttributes redirectAttributes) {
        int count = ingredientService.deleteOrphans();
        redirectAttributes.addFlashAttribute("message", "✅ " + count + " ingrédient(s) orphelin(s) supprimé(s).");
        return "redirect:/admin";
    }

    @PostMapping("/admin/change-password")
    public String changePassword(
            @AuthenticationPrincipal User currentUser,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {

        if (currentPassword.isBlank()) {
            redirectAttributes.addFlashAttribute("errorCurrentPassword", "Ce champ est obligatoire.");
            return "redirect:/admin";
        }
        if (!passwordEncoder.matches(currentPassword, currentUser.getPassword())) {
            redirectAttributes.addFlashAttribute("errorCurrentPassword", "Mot de passe actuel incorrect.");
            return "redirect:/admin";
        }
        if (newPassword.isBlank()) {
            redirectAttributes.addFlashAttribute("errorNewPassword", "Ce champ est obligatoire.");
            return "redirect:/admin";
        }
        if (newPassword.length() < 8) {
            redirectAttributes.addFlashAttribute("errorNewPassword", "Le mot de passe doit contenir au moins 8 caractères.");
            return "redirect:/admin";
        }
        if (confirmPassword.isBlank() || !newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorConfirmPassword", "Les mots de passe ne correspondent pas.");
            return "redirect:/admin";
        }

        User user = userRepository.findById(currentUser.getId()).orElseThrow();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("✅ Password changed for user {}", user.getUsername());
        redirectAttributes.addFlashAttribute("message", "✅ Mot de passe modifié avec succès.");
        return "redirect:/admin";
    }
}