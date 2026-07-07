package com.yt.projetos.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseCleanupRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try (java.sql.Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            String dbName = conn.getMetaData().getDatabaseProductName();
            if (dbName != null && dbName.toLowerCase().contains("sqlite")) {
                log.info("Banco de dados SQLite detectado. Ignorando limpeza de dados legados do PostgreSQL.");
                return;
            }
        } catch (Exception e) {
            log.warn("Não foi possível verificar o nome do banco de dados: {}", e.getMessage());
        }

        log.info("Iniciando limpeza de dados legados vazios no banco de dados...");
        try {
            // Corrige campos vazios que quebram a desserialização de JSON pelo Jackson
            int videoIdeasTagsCount = jdbcTemplate.update(
                "UPDATE video_ideas SET tags = NULL WHERE tags::text = '' OR tags::text = '\"\"'"
            );
            int videoIdeasAltTitlesCount = jdbcTemplate.update(
                "UPDATE video_ideas SET alternative_titles = NULL WHERE alternative_titles::text = '' OR alternative_titles::text = '\"\"'"
            );
            int channelsCtaCount = jdbcTemplate.update(
                "UPDATE channels SET cta_templates = NULL WHERE cta_templates::text = '' OR cta_templates::text = '\"\"'"
            );
            int channelsDescCount = jdbcTemplate.update(
                "UPDATE channels SET description_blocks = NULL WHERE description_blocks::text = '' OR description_blocks::text = '\"\"'"
            );
            int channelsChecklistCount = jdbcTemplate.update(
                "UPDATE channels SET checklist_templates = NULL WHERE checklist_templates::text = '' OR checklist_templates::text = '\"\"'"
            );

            // Fix jsonb type casting errors from Hibernate ddl-auto
            try {
                jdbcTemplate.execute("ALTER TABLE IF EXISTS channels ALTER COLUMN checklist_templates TYPE jsonb USING checklist_templates::jsonb");
                jdbcTemplate.execute("ALTER TABLE IF EXISTS channels ALTER COLUMN cta_templates TYPE jsonb USING cta_templates::jsonb");
                jdbcTemplate.execute("ALTER TABLE IF EXISTS channels ALTER COLUMN description_blocks TYPE jsonb USING description_blocks::jsonb");
            } catch (Exception e) {
                log.debug("Colunas JSONB já atualizadas ou erro: " + e.getMessage());
            }

            // Fix users username nullable error from Hibernate ddl-auto
            try {
                jdbcTemplate.execute("ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS username varchar(255)");
                jdbcTemplate.execute("UPDATE users SET username = COALESCE(split_part(email, '@', 1), 'user') || '_' || substr(id::text, 1, 8) WHERE username IS NULL");
                jdbcTemplate.execute("ALTER TABLE IF EXISTS users ALTER COLUMN username SET NOT NULL");
                jdbcTemplate.execute("ALTER TABLE IF EXISTS users ADD CONSTRAINT users_username_unique UNIQUE (username)");
            } catch (Exception e) {
                log.debug("Coluna username já atualizada ou erro (email column might not exist): " + e.getMessage());
                try {
                    jdbcTemplate.execute("UPDATE users SET username = 'user_' || substr(id::text, 1, 8) WHERE username IS NULL");
                    jdbcTemplate.execute("ALTER TABLE IF EXISTS users ALTER COLUMN username SET NOT NULL");
                } catch (Exception innerE) {
                    log.debug("Erro ao tentar fallback para username: " + innerE.getMessage());
                }
            }

            
            log.info("Limpeza concluída. Registros corrigidos: video_ideas.tags={}, video_ideas.alternative_titles={}, channels.cta_templates={}, channels.description_blocks={}, channels.checklist_templates={}",
                     videoIdeasTagsCount, videoIdeasAltTitlesCount, channelsCtaCount, channelsDescCount, channelsChecklistCount);
        } catch (Exception e) {
            log.error("Erro ao rodar limpeza de dados legados vazios no banco: ", e);
        }
    }
}
