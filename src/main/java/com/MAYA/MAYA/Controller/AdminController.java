package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.Service.phyllo.NightlySyncJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final NightlySyncJob nightlySyncJob;

    @Value("${admin.secret-key}")
    private String adminSecretKey;

    /**
     * POST /api/admin/trigger-nightly-sync
     * Header: X-Admin-Key: <secret>
     * Manually triggers the nightly sync job.
     */
    @PostMapping("/trigger-nightly-sync")
    public ResponseEntity<?> triggerNightlySync(@RequestHeader("X-Admin-Key") String adminKey) {
        if (!adminSecretKey.equals(adminKey)) {
            return ResponseEntity.status(403).body(Map.of("error", "Invalid admin key"));
        }

        log.info("Manual nightly sync triggered via admin API");
        nightlySyncJob.runNightlySync();
        return ResponseEntity.ok(Map.of("message", "Nightly sync triggered successfully"));
    }
}
