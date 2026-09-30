package ISFT.CRM.controller;


import ISFT.CRM.entity.Campaign;
import ISFT.CRM.service.CampaignService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping
    public List<Campaign> getAllCampaigns() {
        return campaignService.getAllCampaigns();
    }

    @GetMapping("/{id}")
    public Campaign getCampaignById(@PathVariable Long id) {
        return campaignService.getCampaignById(id)
                .orElseThrow(() ->
                        new RuntimeException("Campaign not found"));
    }

    @PostMapping
    public Campaign createCampaign(@RequestBody Campaign campaign) {
        return campaignService.createCampaign(campaign);
    }

    @PutMapping("/{id}")
    public Campaign updateCampaign(
            @PathVariable Long id,
            @RequestBody Campaign campaignDetails) {

        return campaignService.updateCampaign(id, campaignDetails);
    }

    @DeleteMapping("/{id}")
    public void deleteCampaign(@PathVariable Long id) {
        campaignService.deleteCampaign(id);
    }
}