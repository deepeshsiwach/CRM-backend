package ISFT.CRM.controller;

import ISFT.CRM.entity.Lead;
import ISFT.CRM.service.LeadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping
    public List<Lead> getAllLeads() {
        return leadService.getLeadsForCurrentUser();
    }

    @GetMapping("/campaign/{campaignId}")
    public List<Lead> getLeadsByCampaign(@PathVariable Long campaignId) {
        return leadService.getLeadsByCampaign(campaignId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lead> getLeadById(@PathVariable Long id) {
        return leadService.getLeadById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/activity")
    public Map<String, Object> getLeadActivity(
            @PathVariable Long id) {

        Map<String, Object> activity =
                new HashMap<>();

        activity.put("leadId", id);

        return activity;
    }

    @PostMapping
    public Lead createLead(@RequestBody Lead lead) {
        return leadService.createLead(lead);
    }

    @PutMapping("/{id}")
    public Lead updateLead(
            @PathVariable Long id,
            @RequestBody Lead leadDetails) {

        return leadService.updateLead(id, leadDetails);
    }


    @PutMapping("/{id}/status")
    public Lead updateLeadStatus(
            @PathVariable Long id,
            @RequestBody Lead.LeadStatus status ) {

        return leadService.updateLeadStatus(id, status);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLead(@PathVariable Long id) {

        leadService.deleteLead(id);

        return ResponseEntity.noContent().build();
    }
}
