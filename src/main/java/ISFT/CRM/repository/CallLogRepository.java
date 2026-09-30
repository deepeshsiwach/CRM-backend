package ISFT.CRM.repository;

import ISFT.CRM.entity.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CallLogRepository extends JpaRepository<CallLog, Long> {

    List<CallLog> findByLeadId(Long leadId);
    List<CallLog> findByLeadIdIn(List<Long> leadIds);
    void deleteByLeadId(Long leadId);

    List<CallLog> findByAgentId(Long agentId);

    List<CallLog> findByCallStatus(CallLog.CallStatus callStatus);

    List<CallLog> findByCallOutcome(CallLog.CallOutcome callOutcome);
}