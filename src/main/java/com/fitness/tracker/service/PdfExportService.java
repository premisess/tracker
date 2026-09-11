package com.fitness.tracker.service;

import com.fitness.tracker.entity.User;
import com.fitness.tracker.entity.Workout;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.repository.WorkoutRepository;
import com.fitness.tracker.security.SecurityUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfExportService {

    private static final float MARGIN = 50;
    private static final float LINE_HEIGHT = 16;
    private static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;

    public PdfExportService(WorkoutRepository workoutRepository, UserRepository userRepository) {
        this.workoutRepository = workoutRepository;
        this.userRepository = userRepository;
    }

    public byte[] exportMyWorkouts() throws IOException {
        User user = getCurrentUser();
        List<Workout> workouts = workoutRepository.findByUserIdOrderByDateDesc(user.getId());

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream content = new PDPageContentStream(document, page);
            float y = page.getMediaBox().getHeight() - MARGIN;

            y = writeLine(content, FONT_BOLD, 16, MARGIN, y, "FitTracker - Workout Report");
            y = writeLine(content, FONT, 11, MARGIN, y, user.getName() + " (" + user.getEmail() + ")");
            y -= LINE_HEIGHT / 2;

            int totalCalories = 0;
            int totalDuration = 0;

            y = writeLine(content, FONT_BOLD, 11, MARGIN, y,
                    pad("Date", 12) + pad("Type", 16) + pad("Duration", 10) + pad("Calories", 10) + "Notes");

            for (Workout w : workouts) {
                if (y < MARGIN + LINE_HEIGHT) {
                    content.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    y = page.getMediaBox().getHeight() - MARGIN;
                }

                int calories = w.getCaloriesBurned() != null ? w.getCaloriesBurned() : 0;
                int duration = w.getDuration() != null ? w.getDuration() : 0;
                totalCalories += calories;
                totalDuration += duration;

                String date = w.getDate() != null ? w.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "-";
                String notes = w.getNotes() != null ? w.getNotes() : "";
                if (notes.length() > 40) notes = notes.substring(0, 37) + "...";

                y = writeLine(content, FONT, 10, MARGIN, y,
                        pad(date, 12) + pad(w.getType(), 16) + pad(duration + "m", 10) + pad(calories + "", 10) + notes);
            }

            y -= LINE_HEIGHT / 2;
            writeLine(content, FONT_BOLD, 11, MARGIN, y,
                    "Total: " + workouts.size() + " workouts, " + totalDuration + " minutes, " + totalCalories + " calories");

            content.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private float writeLine(PDPageContentStream content, PDType1Font font, float size, float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
        return y - LINE_HEIGHT;
    }

    private String pad(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width - 1) + " ";
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
