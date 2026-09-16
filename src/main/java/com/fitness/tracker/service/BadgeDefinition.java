package com.fitness.tracker.service;

/**
 * Every badge a user can earn: a statistic and the value it has to reach. Renaming a constant would
 * orphan badges already stored under the old code, so add new ones instead.
 */
public enum BadgeDefinition {

    FIRST_WORKOUT("First Step", "Log your first workout", "Workouts", "fitness", Metric.WORKOUTS, 1),
    WORKOUTS_10("Getting Serious", "Log 10 workouts", "Workouts", "fitness", Metric.WORKOUTS, 10),
    WORKOUTS_50("Half Century", "Log 50 workouts", "Workouts", "fitness", Metric.WORKOUTS, 50),
    WORKOUTS_100("Century Club", "Log 100 workouts", "Workouts", "trophy", Metric.WORKOUTS, 100),

    STREAK_3("On a Roll", "Work out 3 days in a row", "Streaks", "fire", Metric.LONGEST_STREAK, 3),
    STREAK_7("Week Warrior", "Work out 7 days in a row", "Streaks", "fire", Metric.LONGEST_STREAK, 7),
    STREAK_30("Unstoppable", "Work out 30 days in a row", "Streaks", "fire", Metric.LONGEST_STREAK, 30),

    FIRST_GPS_ACTIVITY("Hit the Road", "Record your first GPS activity", "Running", "run", Metric.GPS_ACTIVITIES, 1),
    RUN_5K("5K Finisher", "Run 5 km in a single GPS run", "Running", "run", Metric.LONGEST_RUN_M, 5_000),
    RUN_10K("10K Club", "Run 10 km in a single GPS run", "Running", "medal", Metric.LONGEST_RUN_M, 10_000),
    DISTANCE_100K("100 km Explorer", "Cover 100 km across all your GPS activities", "Running", "map", Metric.TOTAL_GPS_DISTANCE_M, 100_000),

    FIRST_STRENGTH("Iron Initiate", "Log a workout with exercises and sets", "Strength", "dumbbell", Metric.STRENGTH_SESSIONS, 1),
    STRENGTH_25("Iron Regular", "Log 25 workouts with exercises and sets", "Strength", "dumbbell", Metric.STRENGTH_SESSIONS, 25),

    FIRST_GOAL("Goal Getter", "Complete a goal", "Goals", "target", Metric.GOALS_COMPLETED, 1),
    GOALS_5("Goal Crusher", "Complete 5 goals", "Goals", "target", Metric.GOALS_COMPLETED, 5),

    FOOD_LOG_7("Mindful Eater", "Log your food on 7 different days", "Nutrition", "food", Metric.FOOD_LOG_DAYS, 7),
    FOOD_LOG_30("Nutrition Pro", "Log your food on 30 different days", "Nutrition", "food", Metric.FOOD_LOG_DAYS, 30),
    HYDRATION_7("Hydration Hero", "Log your water on 7 different days", "Nutrition", "water", Metric.WATER_LOG_DAYS, 7),

    PLAN_FIRST_SESSION("Plan Starter", "Complete a session from a workout plan", "Plans", "plan", Metric.PLAN_SESSIONS, 1),
    PLAN_FINISHER("Program Graduate", "Finish a whole workout plan", "Plans", "trophy", Metric.PLANS_COMPLETED, 1),

    EMAIL_VERIFIED("Verified", "Confirm your email address", "Account", "verified", Metric.EMAIL_VERIFIED, 1);

    public enum Metric {
        WORKOUTS, LONGEST_STREAK, GPS_ACTIVITIES, LONGEST_RUN_M, TOTAL_GPS_DISTANCE_M, STRENGTH_SESSIONS,
        GOALS_COMPLETED, FOOD_LOG_DAYS, WATER_LOG_DAYS, PLAN_SESSIONS, PLANS_COMPLETED, EMAIL_VERIFIED
    }

    private final String title;
    private final String description;
    private final String category;
    // A short icon key the website maps to an icon.
    private final String icon;
    private final Metric metric;
    private final long target;

    BadgeDefinition(String title, String description, String category, String icon, Metric metric, long target) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.icon = icon;
        this.metric = metric;
        this.target = target;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getIcon() {
        return icon;
    }

    public Metric getMetric() {
        return metric;
    }

    public long getTarget() {
        return target;
    }
}
