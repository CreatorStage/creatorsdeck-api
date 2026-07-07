package com.yt.projetos.dto;

import com.yt.projetos.model.ReferenceType;

public record ChannelReferenceLinkRequest(
        String title,
        String url,
        String note,
        String thumbnailUrl,
        ReferenceType type,
        String scrapeChannelUrl
) {
}