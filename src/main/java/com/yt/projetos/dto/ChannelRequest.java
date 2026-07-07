package com.yt.projetos.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ChannelRequest(
        @NotBlank(message = "O nome do canal não pode ser vazio")
        String name,
        
        @NotBlank(message = "O nicho não pode ser vazio")
        String niche,
        
        List<String> ctaTemplates,
        String descriptionBlocks,
        String checklistTemplates,
        String description,
        String channelUrl
) {}
