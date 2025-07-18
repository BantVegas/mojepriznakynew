package com.bantvegas.mojepriznakynew.controller;

import com.bantvegas.mojepriznakynew.dto.FreeformRequest;
import com.bantvegas.mojepriznakynew.enums.SubscriptionTier;
import com.bantvegas.mojepriznakynew.model.User;
import com.bantvegas.mojepriznakynew.repository.UserRepository;
import com.bantvegas.mojepriznakynew.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FreeformController {

    private final UserRepository userRepository;
    private final OpenAiService openAiService;

    @PostMapping("/freeform")
    public ResponseEntity<?> handleFreeform(@RequestBody FreeformRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("Neautorizovaný prístup");
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body("Používateľ neexistuje");
        }

        // Ak je používateľ PACIENT, skontroluj limit
        if (user.getSubscriptionTier() == SubscriptionTier.PACIENT && user.getAiUsageCount() >= 2) {
            return ResponseEntity.status(403).body("Limit 2 analýz bol vyčerpaný. Pre upgrade prejdite na PREMIUM.");
        }

        // Volanie OpenAI
        String aiAnswer = openAiService.askChatGPT(request.getPrompt());

        // Aktualizuj použitie
        user.setAiUsageCount(user.getAiUsageCount() + 1);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("result", aiAnswer));
    }
}
