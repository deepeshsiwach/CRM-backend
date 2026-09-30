package ISFT.CRM.controller;


import ISFT.CRM.entity.CallLog;
import ISFT.CRM.service.CallLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

        import java.util.List;

@RestController
@RequestMapping("/api/call-logs")
public class CallLogController {

    private final CallLogService callLogService;

    public CallLogController(CallLogService callLogService) {
        this.callLogService = callLogService;
    }

    @GetMapping
    public List<CallLog> getAllCallLogs() {
        return callLogService.getAllCallLogs();
    }
    @GetMapping("/lead/{leadId}")
    public List<CallLog> getCallLogsByLead(
            @PathVariable Long leadId) {

        return callLogService.getCallLogsByLead(leadId);
    }

    @GetMapping("/campaign/{campaignId}")
    public List<CallLog> getCallLogsByCampaign(
            @PathVariable Long campaignId) {

        return callLogService.getCallLogsByCampaign(campaignId);
    }

    @GetMapping("/agent/{agentId}")
    public List<CallLog> getCallLogsByAgent(
            @PathVariable Long agentId) {

        return callLogService.getCallLogsByAgent(agentId);
    }

    @GetMapping("/status/{callStatus}")
    public List<CallLog> getCallLogsByStatus(
            @PathVariable CallLog.CallStatus callStatus) {

        return callLogService.getCallLogsByStatus(callStatus);
    }
    @GetMapping("/outcome/{callOutcome}")
    public List<CallLog> getCallLogsByOutcome(
            @PathVariable CallLog.CallOutcome callOutcome) {

        return callLogService.getCallLogsByOutcome(callOutcome);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CallLog> getCallLogById(@PathVariable Long id) {
        return callLogService.getCallLogById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public CallLog updateCallLog(
            @PathVariable Long id,
            @RequestBody CallLog callLogDetails) {

        return callLogService.updateCallLog(id, callLogDetails);
    }

    @PostMapping
    public CallLog createCallLog(@RequestBody CallLog callLog) {
        return callLogService.createCallLog(callLog);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCallLog(@PathVariable Long id) {
        callLogService.deleteCallLog(id);
        return ResponseEntity.noContent().build();
    }
}