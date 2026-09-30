package ISFT.CRM.repository;

import ISFT.CRM.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    List<Lead> findByCampaignId(Long campaignId);

    boolean existsByPhone(String phone);
    boolean existsByPhoneAndIdNot(String phone, Long id);

}