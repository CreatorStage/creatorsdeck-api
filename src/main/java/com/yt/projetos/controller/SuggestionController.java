package com.yt.projetos.controller;

import com.yt.projetos.model.ChannelReferenceLink;
import com.yt.projetos.model.SuggestedVideo;
import com.yt.projetos.model.User;
import com.yt.projetos.service.ChannelService;
import com.yt.projetos.service.SuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SuggestionController {

    private final SuggestionService suggestionService;
    private final ChannelService channelService;

    @GetMapping("/{channelId}/suggestions")
    public List<SuggestedVideo> getSuggestions(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID channelId) {
        channelService.getChannel(currentUser, channelId);
        return suggestionService.getSuggestionsForChannel(channelId);
    }

    @DeleteMapping("/{channelId}/suggestions/{videoId}")
    public ResponseEntity<Void> deleteSuggestion(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID channelId,
            @PathVariable UUID videoId) {
        suggestionService.deleteSuggestion(currentUser, channelId, videoId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retorna o status de scraping de cada canal de referência:
     * quais já foram processados e quais ainda estão pendentes.
     */
    @GetMapping("/{channelId}/suggestions/status")
    public ResponseEntity<List<Map<String, Object>>> getSuggestionStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID channelId) {

        channelService.getChannel(currentUser, channelId);
        List<ChannelReferenceLink> refs = channelService.getReferenceLinks(currentUser, channelId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (ChannelReferenceLink ref : refs) {
            String url = ref.getUrl();
            if (url == null) continue;
            boolean isYoutubeChannel = url.contains("youtube.com/@")
                    || url.contains("youtube.com/channel/")
                    || url.contains("youtube.com/c/");
            if (!isYoutubeChannel) continue;

            boolean scraped = suggestionService.isAlreadyScraped(url, channelId);

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("referenceId", ref.getId());
            entry.put("title", ref.getTitle());
            entry.put("url", url);
            entry.put("thumbnailUrl", ref.getThumbnailUrl());
            entry.put("scraped", scraped);
            result.add(entry);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Dispara o scraping em background para todos os canais de referência
     * do channel que ainda não possuem sugestões salvas.
     */
    @PostMapping("/{channelId}/suggestions/sync")
    public ResponseEntity<Map<String, Object>> syncSuggestions(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID channelId) {

        var channel = channelService.getChannel(currentUser, channelId);
        List<ChannelReferenceLink> refs = channelService.getReferenceLinks(currentUser, channelId);

        long queued = 0;
        for (ChannelReferenceLink ref : refs) {
            String url = ref.getUrl();
            if (url != null && (url.contains("youtube.com/@") || url.contains("youtube.com/channel/") || url.contains("youtube.com/c/"))) {
                suggestionService.scrapeSuggestionsForChannel(channel, url, ref.getTitle(), true);
                queued++;
            }
        }

        return ResponseEntity.ok(Map.of(
                "message", "Sync iniciado em background",
                "queued", queued
        ));
    }
}
