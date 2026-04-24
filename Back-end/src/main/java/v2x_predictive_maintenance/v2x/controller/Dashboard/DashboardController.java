package v2x_predictive_maintenance.v2x.controller.Dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import v2x_predictive_maintenance.v2x.dto.Dashboard.AbnormalDeviceDTO;
import v2x_predictive_maintenance.v2x.dto.Dashboard.DashboardSummaryDTO;
import v2x_predictive_maintenance.v2x.dto.Dashboard.MapMarkerDTO;
import v2x_predictive_maintenance.v2x.dto.Dashboard.SpatLatencyDTO;
import v2x_predictive_maintenance.v2x.service.Dashboard.DashboardService;
import v2x_predictive_maintenance.v2x.service.Dashboard.MapService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final MapService mapService;

    @GetMapping("/summary")
    public DashboardSummaryDTO getDashboardSummary() {
        return dashboardService.getDashboardSummary();
    }

    @GetMapping("/abnormal-list")
    public List<AbnormalDeviceDTO> getAbnormalDeviceList() {
        return dashboardService.getAbnormalDeviceList();
    }

    @GetMapping("/map")
    public List<MapMarkerDTO> getMapMarkers() {
        return mapService.getMapMarkers();
    }

    @GetMapping("/spat-latency")
    public SpatLatencyDTO getSpatLatency(
            @RequestParam(value = "intersectionId", required = false) String intersectionId) {
        return dashboardService.getSpatLatencyData(intersectionId);
    }

}