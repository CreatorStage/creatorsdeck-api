package com.yt.projetos.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yt.projetos.dto.ChannelReferenceLinkRequest;
import com.yt.projetos.dto.ChannelRequest;
import com.yt.projetos.model.Channel;
import com.yt.projetos.model.ChannelReferenceLink;
import com.yt.projetos.model.Reference;
import com.yt.projetos.model.User;
import com.yt.projetos.model.VideoIdea;
import com.yt.projetos.repository.ChannelReferenceLinkRepository;
import com.yt.projetos.repository.ChannelRepository;
import com.yt.projetos.repository.NoteRepository;
import com.yt.projetos.repository.ReferenceRepository;
import com.yt.projetos.repository.ScriptVersionRepository;
import com.yt.projetos.repository.VideoIdeaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final VideoIdeaRepository videoIdeaRepository;
    private final NoteRepository noteRepository;
    private final ReferenceRepository referenceRepository;
    private final ChannelReferenceLinkRepository channelReferenceLinkRepository;
    private final ScriptVersionRepository scriptVersionRepository;
    private final PasswordEncoder passwordEncoder;
    private final SuggestionService suggestionService;

    public List<Channel> getChannels(User currentUser) {
        return currentUser == null ? List.of() : channelRepository.findByUserId(currentUser.getId());
    }

    @Transactional
    public Channel createChannel(User currentUser, ChannelRequest request) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Channel channel = Channel.builder()
                .name(request.name())
                .niche(request.niche())
                .ctaTemplates(request.ctaTemplates() != null ? request.ctaTemplates() : new java.util.ArrayList<>())
                .descriptionBlocks(request.descriptionBlocks())
                .checklistTemplates(request.checklistTemplates())
                .description(request.description())
                .channelUrl(request.channelUrl())
                .user(currentUser)
                .build();
        return channelRepository.save(channel);
    }

    public Channel getChannel(User currentUser, UUID id) {
        return getOwnedChannel(currentUser, id);
    }

    @Transactional
    public Channel updateChannel(User currentUser, UUID id, ChannelRequest updates) {
        Channel channel = getOwnedChannel(currentUser, id);
        if (updates.name() != null) channel.setName(updates.name());
        if (updates.niche() != null) channel.setNiche(updates.niche());
        if (updates.ctaTemplates() != null) channel.setCtaTemplates(updates.ctaTemplates());
        if (updates.descriptionBlocks() != null) channel.setDescriptionBlocks(updates.descriptionBlocks());
        if (updates.checklistTemplates() != null) channel.setChecklistTemplates(updates.checklistTemplates());
        if (updates.description() != null) channel.setDescription(updates.description());
        if (updates.channelUrl() != null) channel.setChannelUrl(updates.channelUrl());
        return channelRepository.save(channel);
    }

    public List<ChannelReferenceLink> getReferenceLinks(User currentUser, UUID channelId) {
        if (!isOwnedChannel(currentUser, channelId)) {
            return List.of();
        }
        return channelReferenceLinkRepository.findByChannelIdOrderByCreatedAtDesc(channelId);
    }

    public ChannelReferenceLink addReferenceLink(User currentUser, UUID channelId, ChannelReferenceLinkRequest request) {
        Channel channel = getOwnedChannel(currentUser, channelId);
        ChannelReferenceLink link = ChannelReferenceLink.builder()
                .channel(channel)
            .title(request.title())
            .url(request.url())
            .note(request.note())
            .thumbnailUrl(request.thumbnailUrl())
            .type(request.type() != null ? request.type() : com.yt.projetos.model.ReferenceType.LINK)
                .build();
        ChannelReferenceLink saved = channelReferenceLinkRepository.save(link);
        
        String channelUrlToScrape = request.scrapeChannelUrl();
        if (channelUrlToScrape == null || channelUrlToScrape.trim().isEmpty()) {
            if (request.url() != null && (request.url().contains("youtube.com/@") || request.url().contains("youtube.com/channel/") || request.url().contains("youtube.com/c/"))) {
                channelUrlToScrape = request.url();
            }
        }
        
        if (channelUrlToScrape != null && !channelUrlToScrape.trim().isEmpty()) {
            suggestionService.scrapeSuggestionsForChannel(channel, channelUrlToScrape, request.title());
        }
        
        return saved;
    }

    @Transactional
    public ChannelReferenceLink updateReferenceLink(User currentUser, UUID id, ChannelReferenceLinkRequest updates) {
        ChannelReferenceLink link = channelReferenceLinkRepository.findById(id)
                .filter(found -> isOwnedChannel(currentUser, found.getChannel() != null ? found.getChannel().getId() : null))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (updates.title() != null) link.setTitle(updates.title());
        if (updates.url() != null) link.setUrl(updates.url());
        if (updates.note() != null) link.setNote(updates.note());
        if (updates.thumbnailUrl() != null) link.setThumbnailUrl(updates.thumbnailUrl());
        if (updates.type() != null) link.setType(updates.type());

        return channelReferenceLinkRepository.save(link);
    }

    public void deleteReferenceLink(User currentUser, UUID id) {
        ChannelReferenceLink link = channelReferenceLinkRepository.findById(id)
                .filter(found -> isOwnedChannel(currentUser, found.getChannel() != null ? found.getChannel().getId() : null))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        channelReferenceLinkRepository.delete(link);
    }

    @Transactional
    public void deleteChannel(User currentUser, UUID id, String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha é obrigatória");
        }

        Channel channel = getOwnedChannel(currentUser, id);
        if (channel.getUser() == null || !passwordEncoder.matches(password, channel.getUser().getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha incorreta");
        }

        videoIdeaRepository.deleteNotesByChannelId(id);
        videoIdeaRepository.deleteReferencesByChannelId(id);
        videoIdeaRepository.deleteScriptVersionsByChannelId(id);
        videoIdeaRepository.deleteSponsorshipsByChannelId(id);
        videoIdeaRepository.deleteByChannelId(id);

        channelReferenceLinkRepository.deleteByChannelId(id);
        channelRepository.delete(channel);
    }

    private Channel getOwnedChannel(User currentUser, UUID id) {
        if (currentUser == null || id == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return channelRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private boolean isOwnedChannel(User currentUser, UUID channelId) {
        if (currentUser == null || channelId == null) {
            return false;
        }
        return channelRepository.existsByIdAndUserId(channelId, currentUser.getId());
    }
}