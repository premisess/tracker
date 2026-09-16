package com.fitness.tracker.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fitness.tracker.entity.Food;
import com.fitness.tracker.repository.FoodRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Fills the shared food catalog on first startup from data/foods.json: common everyday foods, including
 * East African staples, with approximate per-serving nutrition based on USDA FoodData Central averages.
 * Mixed dishes are typical home-recipe estimates. Does nothing once catalog foods exist.
 */
@Component
@Order(3)
public class FoodCatalogSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FoodCatalogSeeder.class);

    private final FoodRepository foodRepository;
    private final JsonMapper jsonMapper;

    public FoodCatalogSeeder(FoodRepository foodRepository, JsonMapper jsonMapper) {
        this.foodRepository = foodRepository;
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (foodRepository.countByOwnerIsNull() > 0) {
            return;
        }
        List<CatalogFood> entries;
        try (InputStream in = new ClassPathResource("data/foods.json").getInputStream()) {
            entries = jsonMapper.readValue(in, new TypeReference<List<CatalogFood>>() {});
        }
        LocalDateTime now = LocalDateTime.now();
        foodRepository.saveAll(entries.stream().map(entry -> {
            Food food = new Food();
            food.setName(entry.name());
            food.setServingLabel(entry.serving());
            food.setServingGrams(entry.grams());
            food.setCalories(entry.kcal());
            food.setProteinG(entry.protein());
            food.setCarbsG(entry.carbs());
            food.setFatG(entry.fat());
            food.setFiberG(entry.fiber());
            food.setCreatedAt(now);
            return food;
        }).toList());
        log.info("Seeded {} foods into the nutrition catalog", entries.size());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CatalogFood(String name, String serving, Double grams, double kcal, double protein, double carbs,
                       double fat, Double fiber) {
    }
}
