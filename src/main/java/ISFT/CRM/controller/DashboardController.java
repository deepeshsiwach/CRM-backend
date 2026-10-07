package ISFT.CRM.controller;

import ISFT.CRM.entity.CallLog;
import ISFT.CRM.entity.Campaign;
import ISFT.CRM.entity.FollowUp;
import ISFT.CRM.entity.Lead;
import ISFT.CRM.entity.LeadAssignment;
import ISFT.CRM.entity.User;

import ISFT.CRM.repository.CallLogRepository;
import ISFT.CRM.repository.CampaignRepository;
import ISFT.CRM.repository.FollowUpRepository;
import ISFT.CRM.repository.LeadAssignmentRepository;
import ISFT.CRM.repository.LeadRepository;
import ISFT.CRM.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final LeadRepository leadRepository;
    private final FollowUpRepository followUpRepository;
    private final LeadAssignmentRepository leadAssignmentRepository;
    private final UserRepository userRepository;
    private final CallLogRepository callLogRepository;
    private final CampaignRepository campaignRepository;


    public DashboardController(
            LeadRepository leadRepository,
            FollowUpRepository followUpRepository,
            LeadAssignmentRepository leadAssignmentRepository,
            UserRepository userRepository,
            CallLogRepository callLogRepository,
            CampaignRepository campaignRepository) {

        this.leadRepository = leadRepository;
        this.followUpRepository = followUpRepository;
        this.leadAssignmentRepository = leadAssignmentRepository;
        this.userRepository = userRepository;
        this.callLogRepository = callLogRepository;
        this.campaignRepository = campaignRepository;
    }


    // ==========================================================
    // DASHBOARD SUMMARY
    // ==========================================================

    @GetMapping("/summary")
    public Map<String, Object> getDashboardSummary() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (authentication == null ||
                authentication.getAuthorities().isEmpty()) {

            throw new RuntimeException(
                    "Unauthorized access");
        }


        boolean isAgent =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_AGENT"));


        // ======================================================
        // GET ACTIVE ASSIGNMENTS
        // ======================================================

        List<LeadAssignment> activeAssignments =
                leadAssignmentRepository.findByStatus(
                        LeadAssignment.AssignmentStatus.ACTIVE
                );


        // ======================================================
        // AGENT SECURITY FILTER
        // ======================================================

        if (isAgent) {

            String email =
                    authentication.getName();


            Long agentId =
                    userRepository
                            .findByEmail(email)
                            .map(User::getId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Authenticated user not found"
                                    )
                            );


            // ----------------------------------------------
            // ONLY THIS AGENT'S ACTIVE ASSIGNMENTS
            // ----------------------------------------------

            activeAssignments =
                    activeAssignments.stream()
                            .filter(assignment ->
                                    assignment.getAgentId() != null
                                            &&
                                            assignment.getAgentId()
                                                    .equals(agentId)
                            )
                            .toList();
        }


        // ======================================================
        // GET LEADS
        // ======================================================

        List<Lead> leads;


        if (isAgent) {

            // Agent sees ONLY leads assigned to them

            Set<Long> assignedLeadIds =
                    new HashSet<>();


            for (LeadAssignment assignment :
                    activeAssignments) {

                if (assignment.getLeadId() != null) {

                    assignedLeadIds.add(
                            assignment.getLeadId()
                    );
                }
            }


            leads =
                    leadRepository.findAll()
                            .stream()
                            .filter(lead ->
                                    lead.getId() != null
                                            &&
                                            assignedLeadIds.contains(
                                                    lead.getId()
                                            )
                            )
                            .toList();

        } else {

            // Admin / Manager see all leads

            leads =
                    leadRepository.findAll();
        }


        // ======================================================
        // GET FOLLOW-UPS
        // ======================================================

        List<FollowUp> followUps;


        if (isAgent) {

            String email =
                    authentication.getName();


            Long agentId =
                    userRepository
                            .findByEmail(email)
                            .map(User::getId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Authenticated user not found"
                                    )
                            );


            // Agent gets ONLY their follow-ups

            followUps =
                    followUpRepository.findByAgentId(
                            agentId
                    );

        } else {

            // Admin / Manager get all follow-ups

            followUps =
                    followUpRepository.findAll();
        }


        // ======================================================
        // SUMMARY OBJECT
        // ======================================================

        Map<String, Object> summary =
                new LinkedHashMap<>();


        // ======================================================
        // LEAD SUMMARY
        // ======================================================

        summary.put(
                "totalLeads",
                leads.size()
        );


        summary.put(
                "newLeads",
                countLeadStatus(
                        leads,
                        Lead.LeadStatus.NEW
                )
        );


        summary.put(
                "contactedLeads",
                countLeadStatus(
                        leads,
                        Lead.LeadStatus.CONTACTED
                )
        );


        summary.put(
                "interestedLeads",
                countLeadStatus(
                        leads,
                        Lead.LeadStatus.INTERESTED
                )
        );


        summary.put(
                "followUpLeads",
                countLeadStatus(
                        leads,
                        Lead.LeadStatus.FOLLOW_UP
                )
        );


        summary.put(
                "counsellingLeads",
                countLeadStatus(
                        leads,
                        Lead.LeadStatus.COUNSELLING
                )
        );


        summary.put(
                "enrolledLeads",
                countLeadStatus(
                        leads,
                        Lead.LeadStatus.ENROLLED
                )
        );


        summary.put(
                "notInterestedLeads",
                countLeadStatus(
                        leads,
                        Lead.LeadStatus.NOT_INTERESTED
                )
        );


        summary.put(
                "wrongNumberLeads",
                countLeadStatus(
                        leads,
                        Lead.LeadStatus.WRONG_NUMBER
                )
        );


        summary.put(
                "noResponseLeads",
                countLeadStatus(
                        leads,
                        Lead.LeadStatus.NO_RESPONSE
                )
        );


        summary.put(
                "lostLeads",
                countLeadStatus(
                        leads,
                        Lead.LeadStatus.LOST
                )
        );


        // ======================================================
        // ASSIGNMENT SUMMARY
        // ======================================================

        summary.put(
                "assignedLeads",
                activeAssignments.size()
        );


        // IMPORTANT:
        // Agent must NEVER see company-wide unassigned leads.

        if (isAgent) {

            summary.put(
                    "unassignedLeads",
                    0
            );

        } else {

            summary.put(
                    "unassignedLeads",
                    countUnassignedOpenLeads(
                            leads,
                            activeAssignments
                    )
            );
        }


        // ======================================================
        // FOLLOW-UP SUMMARY
        // ======================================================

        summary.put(
                "totalFollowUps",
                followUps.size()
        );


        summary.put(
                "pendingFollowUps",
                countFollowUpStatus(
                        followUps,
                        FollowUp.FollowUpStatus.PENDING
                )
        );


        summary.put(
                "completedFollowUps",
                countFollowUpStatus(
                        followUps,
                        FollowUp.FollowUpStatus.COMPLETED
                )
        );


        summary.put(
                "missedFollowUps",
                countFollowUpStatus(
                        followUps,
                        FollowUp.FollowUpStatus.MISSED
                )
        );


        summary.put(
                "cancelledFollowUps",
                countFollowUpStatus(
                        followUps,
                        FollowUp.FollowUpStatus.CANCELLED
                )
        );

        // ======================================================
        // ADDITIONAL DASHBOARD METRICS (SPEED OPTIMIZATION)
        // ======================================================

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        long todayFollowUps = followUps.stream()
                .filter(fu -> fu.getFollowUpDate() != null
                        && fu.getFollowUpDate().toLocalDate().isEqual(today))
                .count();
        summary.put("todayFollowUps", todayFollowUps);

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (Lead.LeadStatus status : Lead.LeadStatus.values()) {
            statusCounts.put(status.name(), countLeadStatus(leads, status));
        }
        summary.put("leadStatusCounts", statusCounts);

        if (isAgent) {
            String email = authentication.getName();
            Long currentAgentId = userRepository.findByEmail(email).map(User::getId).orElse(null);
            List<CallLog> agentCalls = currentAgentId != null
                    ? callLogRepository.findByAgentId(currentAgentId)
                    : Collections.emptyList();
            summary.put("totalCalls", agentCalls.size());

            Set<Long> attendedLeadIds = agentCalls.stream()
                    .filter(c -> c.getCallStartTime() != null
                            && c.getCallStartTime().toLocalDate().isEqual(today))
                    .map(CallLog::getLeadId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            long attendedLeads = activeAssignments.stream()
                    .map(LeadAssignment::getLeadId)
                    .filter(id -> id != null && attendedLeadIds.contains(id))
                    .count();
            summary.put("attendedLeads", attendedLeads);
            summary.put("remainingLeads", Math.max(0, leads.size() - attendedLeads));
        } else {
            summary.put("totalCalls", callLogRepository.count());
            summary.put("attendedLeads", 0);
            summary.put("remainingLeads", 0);
        }

        // ======================================================
        // MANAGEMENT ANALYTICS
        // ======================================================
        //
        // These MUST NOT be returned to AGENT.
        //
        // Agent:
        //   []
        //
        // Admin / Manager:
        //   Full analytics
        //
        // ======================================================

        if (isAgent) {

            summary.put(
                    "agentLeadDistribution",
                    List.of()
            );


            summary.put(
                    "agentPerformance",
                    List.of()
            );


            summary.put(
                    "campaignPerformance",
                    List.of()
            );


            summary.put(
                    "leadSourcePerformance",
                    List.of()
            );

        } else {

            List<User> activeAgents = getActiveAgents();

            summary.put(
                    "agentLeadDistribution",
                    buildAgentLeadDistribution(
                            activeAssignments,
                            activeAgents
                    )
            );


            summary.put(
                    "agentPerformance",
                    buildAgentPerformance(
                            activeAssignments,
                            activeAgents
                    )
            );


            summary.put(
                    "campaignPerformance",
                    buildCampaignPerformance(
                            leads
                    )
            );


            summary.put(
                    "leadSourcePerformance",
                    buildLeadSourcePerformance(
                            leads
                    )
            );
        }


        // ======================================================
        // RETURN
        // ======================================================

        return summary;
    }


    // ==========================================================
    // AGENT-WISE ACTIVE LEAD DISTRIBUTION
    // ADMIN / MANAGER ONLY
    // ==========================================================

    private List<Map<String, Object>>
    buildAgentLeadDistribution(
            List<LeadAssignment> activeAssignments,
            List<User> activeAgents) {

        List<Map<String, Object>> result =
                new ArrayList<>();


        for (User agent :
                activeAgents) {

            long activeLeadCount =
                    activeAssignments.stream()
                            .filter(assignment ->
                                    assignment.getAgentId() != null
                                            &&
                                            assignment.getAgentId()
                                                    .equals(agent.getId())
                            )
                            .count();


            Map<String, Object> agentData =
                    new LinkedHashMap<>();


            agentData.put(
                    "agentId",
                    agent.getId()
            );


            agentData.put(
                    "agentName",
                    agent.getFullName()
            );


            agentData.put(
                    "activeLeads",
                    activeLeadCount
            );


            result.add(
                    agentData
            );
        }


        return result;
    }


    // ==========================================================
    // AGENT PERFORMANCE
    // ADMIN / MANAGER ONLY
    // ==========================================================

    private List<Map<String, Object>>
    buildAgentPerformance(
            List<LeadAssignment> activeAssignments,
            List<User> activeAgents) {

        List<Map<String, Object>> result =
                new ArrayList<>();

        // PERFORMANCE OPTIMIZATION: Fetch all calls once in 1 query and group by agentId in memory
        List<CallLog> allCalls =
                callLogRepository.findAll();

        Map<Long, List<CallLog>> callsByAgent =
                allCalls.stream()
                        .filter(call -> call.getAgentId() != null)
                        .collect(Collectors.groupingBy(CallLog::getAgentId));

        for (User agent :
                activeAgents) {

            Long agentId =
                    agent.getId();


            // ------------------------------------------
            // ACTIVE LEADS
            // ------------------------------------------

            long activeLeadCount =
                    activeAssignments.stream()
                            .filter(assignment ->
                                    assignment.getAgentId() != null
                                            &&
                                            assignment.getAgentId()
                                                    .equals(agentId)
                            )
                            .count();


            // ------------------------------------------
            // CALL LOGS (from in-memory map)
            // ------------------------------------------

            List<CallLog> agentCalls =
                    callsByAgent.getOrDefault(agentId, Collections.emptyList());


            long totalCalls =
                    agentCalls.size();


            // ------------------------------------------
            // ENROLLED LEADS
            // ------------------------------------------

            Set<Long> enrolledLeadIds =
                    new HashSet<>();


            for (CallLog callLog :
                    agentCalls) {

                if (
                        callLog.getCallOutcome() ==
                                CallLog.CallOutcome.ENROLLED

                                &&

                                callLog.getLeadId() != null
                ) {

                    enrolledLeadIds.add(
                            callLog.getLeadId()
                    );
                }
            }


            long enrolledLeads =
                    enrolledLeadIds.size();


            // ------------------------------------------
            // PERFORMANCE OBJECT
            // ------------------------------------------

            Map<String, Object> agentData =
                    new LinkedHashMap<>();


            agentData.put(
                    "agentId",
                    agentId
            );


            agentData.put(
                    "agentName",
                    agent.getFullName()
            );


            agentData.put(
                    "activeLeads",
                    activeLeadCount
            );


            agentData.put(
                    "totalCalls",
                    totalCalls
            );


            agentData.put(
                    "enrolledLeads",
                    enrolledLeads
            );


            result.add(
                    agentData
            );
        }


        return result;
    }


    // ==========================================================
    // CAMPAIGN PERFORMANCE
    // ADMIN / MANAGER ONLY
    // ==========================================================

    private List<Map<String, Object>>
    buildCampaignPerformance(List<Lead> leads) {

        List<Campaign> campaigns =
                campaignRepository.findAll();


        List<Map<String, Object>> result =
                new ArrayList<>();

        // PERFORMANCE OPTIMIZATION: Group already loaded leads by campaignId in memory
        Map<Long, List<Lead>> leadsByCampaign =
                leads.stream()
                        .filter(lead -> lead.getCampaignId() != null)
                        .collect(Collectors.groupingBy(Lead::getCampaignId));

        for (Campaign campaign :
                campaigns) {

            if (campaign.getId() == null) {

                continue;
            }


            List<Lead> campaignLeads =
                    leadsByCampaign.getOrDefault(campaign.getId(), Collections.emptyList());


            long totalCampaignLeads =
                    campaignLeads.size();


            long enrolledCampaignLeads =
                    campaignLeads.stream()
                            .filter(lead ->
                                    lead.getStatus() ==
                                            Lead.LeadStatus.ENROLLED
                            )
                            .count();


            Map<String, Object> campaignData =
                    new LinkedHashMap<>();


            campaignData.put(
                    "campaignId",
                    campaign.getId()
            );


            campaignData.put(
                    "campaignName",
                    campaign.getCampaignName()
            );


            campaignData.put(
                    "source",
                    campaign.getSource()
            );


            campaignData.put(
                    "status",
                    campaign.getStatus()
            );


            campaignData.put(
                    "totalLeads",
                    totalCampaignLeads
            );


            campaignData.put(
                    "enrolledLeads",
                    enrolledCampaignLeads
            );


            result.add(
                    campaignData
            );
        }


        return result;
    }


    // ==========================================================
    // LEAD SOURCE PERFORMANCE
    // ADMIN / MANAGER ONLY
    // ==========================================================

    private List<Map<String, Object>>
    buildLeadSourcePerformance(
            List<Lead> leads) {

        Map<String, List<Lead>> leadsBySource =
                new LinkedHashMap<>();


        for (Lead lead :
                leads) {

            String source =
                    lead.getLeadSource();


            if (
                    source == null ||
                            source.trim().isEmpty()
            ) {

                source = "Unknown";

            } else {

                source =
                        source.trim();
            }


            leadsBySource
                    .computeIfAbsent(
                            source,
                            key ->
                                    new ArrayList<>()
                    )
                    .add(lead);
        }


        List<Map<String, Object>> result =
                new ArrayList<>();


        for (
                Map.Entry<String, List<Lead>> entry :
                leadsBySource.entrySet()
        ) {

            String source =
                    entry.getKey();


            List<Lead> sourceLeads =
                    entry.getValue();


            long totalLeads =
                    sourceLeads.size();


            long enrolledLeads =
                    sourceLeads.stream()
                            .filter(lead ->
                                    lead.getStatus() ==
                                            Lead.LeadStatus.ENROLLED
                            )
                            .count();


            double conversionRate =
                    totalLeads == 0
                            ? 0.0
                            :
                            (
                                    (double)
                                            enrolledLeads
                                            /
                                            totalLeads
                            ) * 100.0;


            Map<String, Object> sourceData =
                    new LinkedHashMap<>();


            sourceData.put(
                    "source",
                    source
            );


            sourceData.put(
                    "totalLeads",
                    totalLeads
            );


            sourceData.put(
                    "enrolledLeads",
                    enrolledLeads
            );


            sourceData.put(
                    "conversionRate",
                    Math.round(
                            conversionRate * 100.0
                    ) / 100.0
            );


            result.add(
                    sourceData
            );
        }


        return result;
    }


    // ==========================================================
    // GET ACTIVE AGENTS
    // ==========================================================

    private List<User>
    getActiveAgents() {

        return userRepository
                .findAll()
                .stream()
                .filter(user ->
                        user.getRole() ==
                                User.Role.AGENT
                )
                .filter(user ->
                        user.getStatus() ==
                                User.Status.ACTIVE
                )
                .toList();
    }


    // ==========================================================
    // COUNT LEADS BY STATUS
    // ==========================================================

    private long countLeadStatus(
            List<Lead> leads,
            Lead.LeadStatus status) {

        return leads.stream()
                .filter(lead ->
                        lead.getStatus() ==
                                status
                )
                .count();
    }


    // ==========================================================
    // COUNT FOLLOW-UPS BY STATUS
    // ==========================================================

    private long countFollowUpStatus(
            List<FollowUp> followUps,
            FollowUp.FollowUpStatus status) {

        return followUps.stream()
                .filter(followUp ->
                        followUp.getStatus() ==
                                status
                )
                .count();
    }


    // ==========================================================
    // COUNT OPEN LEADS WITHOUT ACTIVE ASSIGNMENT
    // ==========================================================

    private long countUnassignedOpenLeads(
            List<Lead> leads,
            List<LeadAssignment> activeAssignments) {

        Set<Long> assignedLeadIds =
                new HashSet<>();


        for (
                LeadAssignment assignment :
                activeAssignments
        ) {

            if (assignment.getLeadId() != null) {

                assignedLeadIds.add(
                        assignment.getLeadId()
                );
            }
        }


        return leads.stream()
                .filter(lead ->
                        lead.getId() != null
                                &&
                                !assignedLeadIds.contains(
                                        lead.getId()
                                )
                                &&
                                !isClosedLead(
                                        lead
                                )
                )
                .count();
    }


    // ==========================================================
    // CLOSED LEAD CHECK
    // ==========================================================

    private boolean isClosedLead(
            Lead lead) {

        if (lead.getStatus() == null) {

            return false;
        }


        return
                lead.getStatus() ==
                        Lead.LeadStatus.ENROLLED

                        ||

                        lead.getStatus() ==
                                Lead.LeadStatus.NOT_INTERESTED

                        ||

                        lead.getStatus() ==
                                Lead.LeadStatus.LOST

                        ||

                        lead.getStatus() ==
                                Lead.LeadStatus.WRONG_NUMBER;
    }
}