package by.vstu.dean;

import by.vstu.dean.core.configs.WebSocketConfig;
import by.vstu.dean.core.configs.security.SecurityConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
@EnableCaching
@Import({SecurityConfiguration.class, WebSocketConfig.class})
public class TimetableApplication {
    @Autowired
    private CacheManager cacheManager;

    private static final Logger log = LoggerFactory.getLogger(TimetableApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(TimetableApplication.class, args);
    }

//    @Scheduled(cron = "0 30 12 * * *")
//    @Scheduled(fixedRateString = "PT5M")
//    public void clearCache() {
//        log.info("!!! Запланированная очистка кэша !!!");
//        List<String> cacheNames = cacheManager.getCacheNames().stream().toList();
//        log.info("Существующий кэш: {}", cacheNames);
//        cacheNames.forEach(cacheName-> Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());
//        log.info("Очистка кэша завершена.");
//    }
}
