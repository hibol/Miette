package com.hibol.miette.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hibol.miette.entity.User;
import com.hibol.miette.repository.UserRepository;
import com.hibol.miette.service.IngredientService;
import com.hibol.miette.service.RecipeIndexingService;

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

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("orphanIngredients", ingredientService.findOrphans());
        return "admin";
    }

    @PostMapping("/admin/search/reindex")
    public String reindex(RedirectAttributes redirectAttributes) {
        indexingService.rebuildIndex();
        redirectAttributes.addFlashAttribute("message", "✅ Index de recherche reconstruit !");
        return "redirect:/recettes";
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