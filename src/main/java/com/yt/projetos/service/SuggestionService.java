package com.yt.projetos.service;

import com.yt.projetos.config.RabbitMQConfig;
import com.yt.projetos.model.Channel;
import com.yt.projetos.model.SuggestedVideo;
import com.yt.projetos.model.User;
import com.yt.projetos.repository.ChannelRepository;
import com.yt.projetos.repository.SuggestedVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SuggestionService {

    private final SuggestedVideoRepository suggestedVideoRepository;
    private final ChannelRepository channelRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public void scrapeSuggestionsForChannel(Channel channel, String sourceChannelUrl, String sourceChannelName, boolean force) {
        log.info("Iniciando publicação de solicitação de scraping para URL: {} (force={})", sourceChannelUrl, force);

        if (force) {
            log.info("Forçando sincronização: deletando sugestões antigas de {} para o canal {}", sourceChannelUrl, channel.getId());
            suggestedVideoRepository.deleteBySourceChannelUrlAndChannelId(sourceChannelUrl, channel.getId());
        } else {
            // Verifica se já não existem sugestões deste canal (evita duplicidade)
            if (suggestedVideoRepository.existsBySourceChannelUrlAndChannelId(sourceChannelUrl, channel.getId())) {
                log.info("Já existem sugestões do canal {} para o channel_id {}. Pulando publicação.", sourceChannelUrl, channel.getId());
                return;
            }
        }

        try {
            Map<String, Object> message = Map.of(
                "channelId", channel.getId().toString(),
                "sourceChannelUrl", sourceChannelUrl,
                "sourceChannelName", sourceChannelName != null ? sourceChannelName : ""
            );

            rabbitTemplate.convertAndSend(RabbitMQConfig.SCRAPE_REQUESTS_QUEUE, message);
            log.info("Mensagem de scraping publicada com sucesso na fila {}", RabbitMQConfig.SCRAPE_REQUESTS_QUEUE);
        } catch (Exception e) {
            log.error("Erro ao publicar solicitação de scraping no RabbitMQ: ", e);
        }
    }

    @Transactional
    public void saveSuggestions(java.util.UUID channelId, String sourceChannelUrl, String sourceChannelName, List<Map<String, String>> videosData) {
        log.info("Salvando {} sugestões recebidas para o canal {}", videosData.size(), channelId);
        Channel channel = channelRepository.findById(channelId).orElse(null);
        if (channel == null) {
            log.error("Canal não encontrado para o ID: {}", channelId);
            return;
        }

        // Verifica se já não existem sugestões deste canal (evita duplicidade de inserção assíncrona tardia)
        if (suggestedVideoRepository.existsBySourceChannelUrlAndChannelId(sourceChannelUrl, channelId)) {
            log.info("Já existem sugestões do canal {} para o channel_id {}. Pulando salvamento.", sourceChannelUrl, channelId);
            return;
        }

        java.util.List<SuggestedVideo> listToSave = new java.util.ArrayList<>();
        for (Map<String, String> videoData : videosData) {
            String title = videoData.get("titulo");
            String url = videoData.get("url_video");
            String views = videoData.get("visualizacoes");
            
            // Tentar extrair o ID do vídeo para montar a thumbnail
            String thumbnailUrl = extractThumbnailUrl(url);

            SuggestedVideo suggestedVideo = SuggestedVideo.builder()
                    .channel(channel)
                    .sourceChannelName(sourceChannelName != null && !sourceChannelName.isBlank() ? sourceChannelName : videoData.get("canal"))
                    .sourceChannelUrl(sourceChannelUrl)
                    .title(title)
                    .url(url)
                    .viewsCount(parseViews(views))
                    .thumbnailUrl(thumbnailUrl)
                    .build();

            listToSave.add(suggestedVideo);
        }
        suggestedVideoRepository.saveAll(listToSave);
        log.info("Processo de sugestão finalizado. {} vídeos salvos no banco.", videosData.size());

        // Despacha para o novo scraper os vídeos com mais de 25.000 visualizações
        for (SuggestedVideo sv : listToSave) {
            if (sv.getViewsCount() != null && sv.getViewsCount() > 25000) {
                try {
                    Map<String, Object> detailsMessage = Map.of(
                        "videoId", sv.getId().toString(),
                        "videoUrl", sv.getUrl()
                    );
                    rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_DETAILS_REQUESTS_QUEUE, detailsMessage);
                    log.info("Solicitação de detalhes enviada para o vídeo {} (views: {})", sv.getId(), sv.getViewsCount());
                } catch (Exception e) {
                    log.error("Erro ao publicar solicitação de detalhes para o vídeo {}: ", sv.getId(), e);
                }
            }
        }
    }

    public List<SuggestedVideo> getSuggestionsForChannel(java.util.UUID channelId) {
        return suggestedVideoRepository.findByChannelIdOrderByCreatedAtDesc(channelId);
    }

    public boolean isAlreadyScraped(String sourceChannelUrl, java.util.UUID channelId) {
        return suggestedVideoRepository.existsBySourceChannelUrlAndChannelId(sourceChannelUrl, channelId);
    }

    @Transactional
    public void deleteSuggestion(User currentUser, UUID channelId, UUID videoId) {
        if (currentUser == null || channelId == null || videoId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parâmetros inválidos");
        }
        
        // Verify channel ownership
        if (!channelRepository.existsByIdAndUserId(channelId, currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado ao canal");
        }

        // Find suggestion
        SuggestedVideo suggestion = suggestedVideoRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sugestão não encontrada"));

        // Verify suggestion belongs to the channel
        if (suggestion.getChannel() == null || !suggestion.getChannel().getId().equals(channelId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sugestão não pertence a este canal");
        }

        suggestedVideoRepository.delete(suggestion);
    }
    
    private String extractThumbnailUrl(String videoUrl) {
        if (videoUrl == null) return null;
        Pattern pattern = Pattern.compile("(?:youtube\\.com\\/(?:[^/]+\\/.+\\/|(?:v|e(?:mbed)?)\\/|.*[?&]v=)|youtu\\.be\\/)([^\"&?/ ]{11})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(videoUrl);
        if (matcher.find()) {
            return "https://img.youtube.com/vi/" + matcher.group(1) + "/hqdefault.jpg";
        }
        return null;
    }

    public static Long parseViews(String viewsStr) {
        if (viewsStr == null || viewsStr.isBlank()) {
            return null;
        }
        try {
            // Match the number and an optional multiplier (mil, mi, bi, k, m, b)
            Pattern pattern = Pattern.compile("([\\d.,]+)\\s*(mil|mi|bi|k|m|b)?(?:\\s|$|[^a-z])", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(viewsStr);
            if (!matcher.find()) return null;
            
            String numStr = matcher.group(1);
            String multiplierStr = matcher.group(2) != null ? matcher.group(2).toLowerCase() : null;
            
            if (numStr.contains(",")) {
                numStr = numStr.replace(".", "").replace(",", ".");
            } else {
                numStr = numStr.replaceAll("\\.(\\d{3})", "$1");
            }
            
            double num = Double.parseDouble(numStr);
            
            if (multiplierStr != null) {
                if (multiplierStr.equals("mil") || multiplierStr.equals("k")) {
                    num *= 1_000.0;
                } else if (multiplierStr.equals("mi") || multiplierStr.equals("m")) {
                    num *= 1_000_000.0;
                } else if (multiplierStr.equals("bi") || multiplierStr.equals("b")) {
                    num *= 1_000_000_000.0;
                }
            }
            return (long) num;
        } catch (Exception e) {
            return null;
        }
    }
}
