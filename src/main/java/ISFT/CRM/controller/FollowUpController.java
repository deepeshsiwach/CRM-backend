package ISFT.CRM.controller;

import ISFT.CRM.entity.FollowUp;
import ISFT.CRM.service.FollowUpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/follow-ups")
public class FollowUpController {

    private final FollowUpService followUpService;

    public FollowUpController(FollowUpService followUpService) {
        this.followUpService = followUpService;
    }

    @GetMapping
    public List<FollowUp> getAllFollowUps() {
        return followUpService.getAllFollowUps();
    }


    // ========================================
    // OVERDUE FOLLOW-UPS
    // ========================================

    @GetMapping("/overdue")
    public List<FollowUp> getOverdueFollowUps() {

        return followUpService.getOverdueFollowUps();
    }


    // ========================================
    // TODAY'S FOLLOW-UPS
    // ========================================

    @GetMapping("/today")
    public List<FollowUp> getTodayFollowUps() {

        return followUpService.getTodayFollowUps();
    }


    // ========================================
    // UPCOMING FOLLOW-UPS
    // ========================================

    @GetMapping("/upcoming")
    public List<FollowUp> getUpcomingFollowUps() {

        return followUpService.getUpcomingFollowUps();
    }


    @GetMapping("/{id}")
    public ResponseEntity<FollowUp> getFollowUpById(
            @PathVariable Long id) {

        return followUpService.getFollowUpById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @PutMapping("/{id}")
    public FollowUp updateFollowUp(
            @PathVariable Long id,
            @RequestBody FollowUp followUpDetails) {

        return followUpService.updateFollowUp(
                id,
                followUpDetails
        );
    }


    @GetMapping("/lead/{leadId}")
    public List<FollowUp> getFollowUpsByLead(
            @PathVariable Long leadId) {

        return followUpService.getFollowUpsByLead(leadId);
    }


    @GetMapping("/agent/{agentId}")
    public List<FollowUp> getFollowUpsByAgent(
            @PathVariable Long agentId) {

        return followUpService.getFollowUpsByAgent(agentId);
    }


    @GetMapping("/status/{status}")
    public List<FollowUp> getFollowUpsByStatus(
            @PathVariable FollowUp.FollowUpStatus status) {

        return followUpService.getFollowUpsByStatus(status);
    }


    @GetMapping("/agent/{agentId}/status/{status}")
    public List<FollowUp> getFollowUpsByAgentAndStatus(
            @PathVariable Long agentId,
            @PathVariable FollowUp.FollowUpStatus status) {

        return followUpService.getFollowUpsByAgentAndStatus(
                agentId,
                status
        );
    }


    @PostMapping
    public FollowUp createFollowUp(
            @RequestBody FollowUp followUp) {

        return followUpService.createFollowUp(followUp);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFollowUp(
            @PathVariable Long id) {

        followUpService.deleteFollowUp(id);

        return ResponseEntity.noContent().build();
    }


    @PutMapping("/{id}/status")
    public FollowUp updateFollowUpStatus(
            @PathVariable Long id,
            @RequestParam FollowUp.FollowUpStatus status) {

        return followUpService.updateFollowUpStatus(
                id,
                status
        );
    }
}