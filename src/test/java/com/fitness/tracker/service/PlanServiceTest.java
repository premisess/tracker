package com.fitness.tracker.service;

import com.fitness.tracker.entity.PlanDay;
import com.fitness.tracker.entity.WorkoutPlan;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlanServiceTest {

    private static PlanDay day(WorkoutPlan plan, Integer week, int dayNumber, String title) {
        PlanDay d = new PlanDay();
        d.setPlan(plan);
        d.setWeekNumber(week);
        d.setDayNumber(dayNumber);
        d.setTitle(title);
        plan.getDays().add(d);
        return d;
    }

    @Test
    void repeatingSessionsCycleThroughTheWeek() {
        WorkoutPlan plan = new WorkoutPlan();
        plan.setSlug("abc");
        plan.setDurationWeeks(8);
        plan.setDaysPerWeek(3);
        day(plan, null, 1, "A");
        day(plan, null, 2, "B");
        day(plan, null, 3, "C");

        assertEquals("A", PlanService.dayFor(plan, 0).getTitle());
        assertEquals("C", PlanService.dayFor(plan, 2).getTitle());
        assertEquals("A", PlanService.dayFor(plan, 3).getTitle());   // week 2, day 1
        assertEquals("B", PlanService.dayFor(plan, 22).getTitle());  // week 8, day 2
        assertEquals(24, plan.totalSessions());
    }

    @Test
    void weekSpecificSessionsWinOverRepeatingOnes() {
        WorkoutPlan plan = new WorkoutPlan();
        plan.setSlug("run");
        plan.setDurationWeeks(2);
        plan.setDaysPerWeek(2);
        day(plan, null, 1, "Easy");
        day(plan, null, 2, "Easy");
        day(plan, 2, 2, "Long run");

        assertEquals("Easy", PlanService.dayFor(plan, 1).getTitle());     // week 1, day 2
        assertEquals("Easy", PlanService.dayFor(plan, 2).getTitle());     // week 2, day 1
        assertEquals("Long run", PlanService.dayFor(plan, 3).getTitle()); // week 2, day 2
    }

    @Test
    void missingSessionIsReported() {
        WorkoutPlan plan = new WorkoutPlan();
        plan.setSlug("broken");
        plan.setDurationWeeks(1);
        plan.setDaysPerWeek(2);
        day(plan, null, 1, "Only day");

        assertThrows(IllegalStateException.class, () -> PlanService.dayFor(plan, 1));
    }
}
