package com.yt.projetos.listener;

import com.yt.projetos.config.RabbitMQConfig;
import com.yt.projetos.model.SuggestedVideo;
import com.yt.projetos.repository.SuggestedVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoDetailsListener {

    private final SuggestedVideoRepository suggestedVideoRepository;

    @RabbitListener(queues = RabbitMQConfig.VIDEO_DETAILS_RESULTS_QUEUE)
    public void handleVideoDetailsResults(Map<String, Object> message) {
        log.info("Mensagem de detalhes de vídeo recebida do RabbitMQ: {}", message);

        try {
            String status = (String) message.get("status");
            String videoIdStr = (String) message.get("videoId");
            
            if (videoIdStr == null) {
                log.error("ID de vídeo ausente na resposta.");
                return;
            }

            if (!"success".equals(status)) {
                log.error("Erro reportado pelo Scraper de Detalhes para o vídeo {}. Mensagem: {}", 
                          videoIdStr, message.get("error"));
                return;
            }

            UUID videoId = UUID.fromString(videoIdStr);
            String publishedAt = (String) message.get("publishedAt");
            Long preciseViewsCount = null;
            if (message.get("preciseViewsCount") != null) {
                if (message.get("preciseViewsCount") instanceof Number) {
                    preciseViewsCount = ((Number) message.get("preciseViewsCount")).longValue();
                } else if (message.get("preciseViewsCount") instanceof String) {
                    preciseViewsCount = Long.parseLong((String) message.get("preciseViewsCount"));
                }
            }

            final String finalPublishedAt = publishedAt;
            final Long finalPreciseViewsCount = preciseViewsCount;

            suggestedVideoRepository.findById(videoId).ifPresent(video -> {
                if (finalPublishedAt != null) {
                    video.setPublishedAt(finalPublishedAt);
                }
                if (finalPreciseViewsCount != null) {
                    video.setPreciseViewsCount(finalPreciseViewsCount);
                }
                video.setPreciseDataCollected(true);
                suggestedVideoRepository.save(video);
                log.info("Detalhes salvos para o vídeo {}: publishedAt={}, preciseViewsCount={}", 
                         videoId, finalPublishedAt, finalPreciseViewsCount);
            });
        } catch (Exception e) {
            log.error("Erro ao processar mensagem de detalhes do vídeo: ", e);
        }
    }
}
