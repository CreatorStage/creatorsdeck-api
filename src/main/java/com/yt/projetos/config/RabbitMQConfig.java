package com.yt.projetos.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String SCRAPE_REQUESTS_QUEUE = "youtube.scrape.requests";
    public static final String SCRAPE_RESULTS_QUEUE = "youtube.scrape.results";
    public static final String VIDEO_DETAILS_REQUESTS_QUEUE = "youtube.scrape.video_details.requests";
    public static final String VIDEO_DETAILS_RESULTS_QUEUE = "youtube.scrape.video_details.results";

    @Bean
    public Queue scrapeRequestsQueue() {
        return new Queue(SCRAPE_REQUESTS_QUEUE, true);
    }

    @Bean
    public Queue scrapeResultsQueue() {
        return new Queue(SCRAPE_RESULTS_QUEUE, true);
    }

    @Bean
    public Queue videoDetailsRequestsQueue() {
        return new Queue(VIDEO_DETAILS_REQUESTS_QUEUE, true);
    }

    @Bean
    public Queue videoDetailsResultsQueue() {
        return new Queue(VIDEO_DETAILS_RESULTS_QUEUE, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
