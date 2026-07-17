package com.yt.projetos.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "suggested_videos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestedVideo {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "source_channel_name")
    private String sourceChannelName;

    @Column(name = "source_channel_url", columnDefinition = "TEXT")
    private String sourceChannelUrl;

    @Column(name = "views_count")
    private Long viewsCount;

    @jakarta.persistence.Transient
    public String getViews() {
        return formatViews(viewsCount);
    }

    public static String formatViews(Long count) {
        if (count == null) return "0";
        if (count >= 1_000_000_000) {
            double val = count / 1_000_000_000.0;
            return String.format(java.util.Locale.US, val % 1 == 0 ? "%.0fB" : "%.1fB", val);
        }
        if (count >= 1_000_000) {
            double val = count / 1_000_000.0;
            return String.format(java.util.Locale.US, val % 1 == 0 ? "%.0fM" : "%.1fM", val);
        }
        if (count >= 1_000) {
            double val = count / 1_000.0;
            return String.format(java.util.Locale.US, val % 1 == 0 ? "%.0fK" : "%.1fK", val);
        }
        return String.valueOf(count);
    }

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private String publishedAt;

    @Column(name = "precise_views_count")
    private Long preciseViewsCount;

    @Column(name = "precise_data_collected")
    private Boolean preciseDataCollected = false;
}
