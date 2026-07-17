package com.yt.projetos.repository;

import com.yt.projetos.model.SuggestedVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SuggestedVideoRepository extends JpaRepository<SuggestedVideo, UUID> {
    List<SuggestedVideo> findByChannelIdOrderByCreatedAtDesc(UUID channelId);
    boolean existsBySourceChannelUrlAndChannelId(String sourceChannelUrl, UUID channelId);
    void deleteBySourceChannelUrlAndChannelId(String sourceChannelUrl, UUID channelId);
}
