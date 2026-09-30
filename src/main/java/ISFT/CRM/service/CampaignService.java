package ISFT.CRM.service;

import ISFT.CRM.entity.Campaign;
import ISFT.CRM.repository.CampaignRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;

    public CampaignService(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    public List<Campaign> getAllCampaigns() {
        return campaignRepository.findAll();
    }

    public Optional<Campaign> getCampaignById(Long id) {
        return campaignRepository.findById(id);
    }

    public Campaign createCampaign(Campaign campaign) {
        if (campaign.getCampaignName() == null ||
                campaign.getCampaignName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Campaign name is required");
        }

        if (campaign.getStartDate() != null &&
                campaign.getEndDate() != null &&
                campaign.getEndDate().isBefore(campaign.getStartDate())) {

            throw new IllegalArgumentException(
                    "Campaign end date cannot be before start date");
        }
        return campaignRepository.save(campaign);
    }

    public Campaign updateCampaign(Long id, Campaign campaignDetails) {

        if (campaignDetails.getCampaignName() == null ||
                campaignDetails.getCampaignName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Campaign name is required");
        }

        if (campaignDetails.getStartDate() != null &&
                campaignDetails.getEndDate() != null &&
                campaignDetails.getEndDate().isBefore(campaignDetails.getStartDate())) {

            throw new IllegalArgumentException(
                    "Campaign end date cannot be before start date");
        }

        Campaign existingCampaign = campaignRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Campaign not found"));

        existingCampaign.setCampaignName(campaignDetails.getCampaignName());
        existingCampaign.setDescription(campaignDetails.getDescription());
        existingCampaign.setSource(campaignDetails.getSource());
        existingCampaign.setCourseId(campaignDetails.getCourseId());
        existingCampaign.setStartDate(campaignDetails.getStartDate());
        existingCampaign.setEndDate(campaignDetails.getEndDate());
        existingCampaign.setStatus(campaignDetails.getStatus());

        return campaignRepository.save(existingCampaign);
    }

    public void deleteCampaign(Long id) {

        if (!campaignRepository.existsById(id)) {
            throw new RuntimeException("Campaign not found");
        }

        campaignRepository.deleteById(id);
    }
}