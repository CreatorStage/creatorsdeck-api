package com.yt.projetos.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "channels")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Channel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String niche;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cta_templates", columnDefinition = "jsonb")
    private List<String> ctaTemplates = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "description_blocks", columnDefinition = "jsonb")
    private String descriptionBlocks;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checklist_templates", columnDefinition = "jsonb")
    private String checklistTemplates;

    @Column(name = "channel_url", columnDefinition = "TEXT")
    private String channelUrl;

    @org.hibernate.annotations.CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}

