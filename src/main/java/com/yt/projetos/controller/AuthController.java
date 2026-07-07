package com.yt.projetos.controller;

import com.yt.projetos.dto.AuthResponse;
import com.yt.projetos.dto.LoginRequest;
import com.yt.projetos.dto.RegisterRequest;
import com.yt.projetos.dto.UserResponse;
import com.yt.projetos.model.User;
import com.yt.projetos.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@jakarta.validation.Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@jakarta.validation.Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal User currentUser) {
        User user = authService.getCurrentUser(currentUser);
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getUsername(), user.getCreatedAt()));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.amqp.rabbit.connection.ConnectionFactory rabbitConnectionFactory;

    @GetMapping("/services-status")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getServicesStatus() {
        java.util.List<java.util.Map<String, Object>> services = new java.util.ArrayList<>();

        // 1. Database SQLite
        java.util.Map<String, Object> dbStatus = new java.util.HashMap<>();
        dbStatus.put("name", "Banco de Dados (SQLite)");
        dbStatus.put("port", "N/A");
        dbStatus.put("details", "Arquivo local: creatorsdeck.db");
        dbStatus.put("status", "running");
        services.add(dbStatus);

        // 2. RabbitMQ Status
        java.util.Map<String, Object> rabbitStatus = new java.util.HashMap<>();
        rabbitStatus.put("name", "RabbitMQ Message Broker");
        rabbitStatus.put("port", "5672");
        
        boolean rabbitRunning = false;
        if (rabbitConnectionFactory != null) {
            try (org.springframework.amqp.rabbit.connection.Connection conn = rabbitConnectionFactory.createConnection()) {
                rabbitStatus.put("status", "running");
                rabbitStatus.put("details", "Conectado com sucesso");
                rabbitRunning = true;
            } catch (Exception e) {
                rabbitStatus.put("status", "error");
                rabbitStatus.put("details", "Erro de conexão: " + e.getMessage());
            }
        } else {
            rabbitStatus.put("status", "stopped");
            rabbitStatus.put("details", "Serviço AMQP não configurado");
        }
        services.add(rabbitStatus);

        // 3. YouTube Scraper Worker
        java.util.Map<String, Object> scraperStatus = new java.util.HashMap<>();
        scraperStatus.put("name", "YouTube Scraper Worker");
        scraperStatus.put("port", "N/A");
        
        boolean scraperRunning = false;
        String scraperDetails = "Sem conexão com RabbitMQ";
        
        if (rabbitRunning && rabbitConnectionFactory != null) {
            try {
                org.springframework.amqp.rabbit.core.RabbitAdmin admin = new org.springframework.amqp.rabbit.core.RabbitAdmin(rabbitConnectionFactory);
                java.util.Properties props = admin.getQueueProperties("youtube.scrape.requests");
                if (props != null) {
                    Integer consumerCount = (Integer) props.get(org.springframework.amqp.rabbit.core.RabbitAdmin.QUEUE_CONSUMER_COUNT);
                    if (consumerCount != null && consumerCount > 0) {
                        scraperRunning = true;
                        scraperDetails = consumerCount + " worker(s) ativo(s) escutando a fila";
                    } else {
                        scraperDetails = "Nenhum worker ativo escutando a fila";
                    }
                } else {
                    scraperDetails = "Fila 'youtube.scrape.requests' não encontrada";
                }
            } catch (Exception e) {
                scraperDetails = "Erro ao verificar fila: " + e.getMessage();
            }
        }
        
        scraperStatus.put("status", scraperRunning ? "running" : "stopped");
        scraperStatus.put("details", scraperDetails);
        services.add(scraperStatus);

        return ResponseEntity.ok(services);
    }
}
