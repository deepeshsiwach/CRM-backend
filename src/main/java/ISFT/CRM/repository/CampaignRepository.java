package ISFT.CRM.repository;



import ISFT.CRM.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    long countByCourseId(Long courseId);
}