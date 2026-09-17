package com.fitness.tracker.controller;

import com.fitness.tracker.security.CurrentUserService;
import com.fitness.tracker.service.PdfExportService;
import com.fitness.tracker.service.UltimateGuard;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final PdfExportService pdfExportService;
    private final CurrentUserService currentUserService;
    private final UltimateGuard ultimateGuard;

    public ExportController(PdfExportService pdfExportService, CurrentUserService currentUserService,
                            UltimateGuard ultimateGuard) {
        this.pdfExportService = pdfExportService;
        this.currentUserService = currentUserService;
        this.ultimateGuard = ultimateGuard;
    }

    @GetMapping("/workouts/pdf")
    public ResponseEntity<byte[]> exportWorkoutsPdf() throws IOException {
        ultimateGuard.require(currentUserService.get(), "PDF workout reports");
        byte[] pdf = pdfExportService.exportMyWorkouts();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename("workout-report.pdf").build());

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
