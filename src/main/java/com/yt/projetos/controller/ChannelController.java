package com.yt.projetos.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yt.projetos.dto.ChannelReferenceLinkRequest;
import com.yt.projetos.dto.ChannelReferenceLinkResponse;
import com.yt.projetos.dto.ChannelResponse;
import com.yt.projetos.dto.ChannelRequest;
import com.yt.projetos.model.Channel;
import com.yt.projetos.model.ChannelReferenceLink;
import com.yt.projetos.model.User;
import com.yt.projetos.service.ChannelService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChannelController {

    private final ChannelService channelService;

    @GetMapping
    public List<ChannelResponse> getChannels(@AuthenticationPrincipal User currentUser) {
        return channelService.getChannels(currentUser).stream().map(this::toChannelResponse).toList();
    }

    @PostMapping
    public ResponseEntity<ChannelResponse> createChannel(@AuthenticationPrincipal User currentUser, @Valid @RequestBody ChannelRequest request) {
        return ResponseEntity.status(201).body(toChannelResponse(channelService.createChannel(currentUser, request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChannelResponse> getChannel(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        return ResponseEntity.ok(toChannelResponse(channelService.getChannel(currentUser, id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChannelResponse> updateChannel(@AuthenticationPrincipal User currentUser, @PathVariable UUID id, @Valid @RequestBody ChannelRequest updates) {
        return ResponseEntity.ok(toChannelResponse(channelService.updateChannel(currentUser, id, updates)));
    }

    @GetMapping("/{channelId}/references")
    public List<ChannelReferenceLinkResponse> getReferenceLinks(@AuthenticationPrincipal User currentUser, @PathVariable UUID channelId) {
        return channelService.getReferenceLinks(currentUser, channelId).stream().map(this::toReferenceLinkResponse).toList();
    }

    @PostMapping("/{channelId}/references")
    public ResponseEntity<ChannelReferenceLinkResponse> addReferenceLink(@AuthenticationPrincipal User currentUser, @PathVariable UUID channelId, @RequestBody ChannelReferenceLinkRequest request) {
        return ResponseEntity.status(201).body(toReferenceLinkResponse(channelService.addReferenceLink(currentUser, channelId, request)));
    }

    @PutMapping("/reference-links/{id}")
    public ResponseEntity<ChannelReferenceLinkResponse> updateReferenceLink(@AuthenticationPrincipal User currentUser, @PathVariable UUID id, @RequestBody ChannelReferenceLinkRequest request) {
        return ResponseEntity.ok(toReferenceLinkResponse(channelService.updateReferenceLink(currentUser, id, request)));
    }

    @DeleteMapping("/reference-links/{id}")
    public ResponseEntity<?> deleteReferenceLink(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        channelService.deleteReferenceLink(currentUser, id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteChannel(@AuthenticationPrincipal User currentUser, @PathVariable UUID id, @RequestBody java.util.Map<String, String> payload) {
        channelService.deleteChannel(currentUser, id, payload.get("password"));
        return ResponseEntity.ok().build();
    }

    private ChannelResponse toChannelResponse(Channel channel) {
        return new ChannelResponse(
                channel.getId(),
                channel.getUser() != null ? channel.getUser().getId() : null,
                channel.getName(),
                channel.getNiche(),
                channel.getCtaTemplates(),
                channel.getDescriptionBlocks(),
                channel.getChecklistTemplates(),
                channel.getDescription(),
                channel.getChannelUrl(),
                channel.getCreatedAt(),
                channel.getDeletedAt()
        );
    }

    private ChannelReferenceLinkResponse toReferenceLinkResponse(ChannelReferenceLink link) {
        return new ChannelReferenceLinkResponse(
                link.getId(),
                link.getChannel() != null ? link.getChannel().getId() : null,
                link.getTitle(),
                link.getUrl(),
                link.getNote(),
                link.getThumbnailUrl(),
                link.getType(),
                link.getCreatedAt()
        );
    }
}
