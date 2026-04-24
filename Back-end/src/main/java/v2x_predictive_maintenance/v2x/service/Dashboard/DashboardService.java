package v2x_predictive_maintenance.v2x.service.Dashboard;



import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;

import org.springframework.stereotype.Service;

import v2x_predictive_maintenance.v2x.dto.Dashboard.AbnormalDeviceDTO;

import v2x_predictive_maintenance.v2x.dto.Dashboard.DashboardSummaryDTO;

import v2x_predictive_maintenance.v2x.dto.Dashboard.SpatLatencyDTO;

import v2x_predictive_maintenance.v2x.entity.Dashboard.IntersectionInfo;

import v2x_predictive_maintenance.v2x.entity.Dashboard.RiskAnalysisResult;

import v2x_predictive_maintenance.v2x.entity.Dashboard.V2XCommunicationLog;

import v2x_predictive_maintenance.v2x.repository.Dashboard.IntersectionInfoRepository;

import v2x_predictive_maintenance.v2x.repository.Dashboard.RiskAnalysisResultRepository;

import v2x_predictive_maintenance.v2x.repository.Dashboard.V2XCommunicationLogRepository;



import java.math.BigDecimal;

import java.math.RoundingMode;

import java.time.Instant;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.LinkedHashSet;

import java.util.List;

import java.util.Map;

import java.util.stream.Collectors;



/**

 * 통합 모니터링 요약 규칙 (대시보드 KPI):

 * <ul>

 *   <li><b>전체 기기</b>: {@code intersection_info}에 등록된 교차로 수. 등록이 없을 때만

 *       {@code risk_analysis_result}에 나타난 교차로 ID로 보조(시뮬만 돌린 경우).</li>

 *   <li><b>정상 / 이상</b>: 각 교차로에 대해 DB에 저장된 <b>최신 위험 분석 1건</b>만 사용.

 *       분석 이력이 없으면 <b>정상 작동</b>으로 집계한다.</li>

 *   <li><b>이상 탐지</b>: 최신 등급이 {@link MonitoringRiskLevel#LABEL_NORMAL}이 아닌 교차로 수.</li>

 *   <li><b>이상 비율</b>: {@code 이상 탐지 / 전체 기기 × 100}(%), 소수 둘째 자리.</li>

 *   <li><b>평균 위험 점수</b>: 등록 교차로마다 최신 {@code total_risk_score}를 쓰고, 미분석 교차로는 0으로 포함한 산술 평균.</li>

 * </ul>

 */

@Service

@RequiredArgsConstructor

public class DashboardService {



    private final IntersectionInfoRepository intersectionInfoRepository;



    private final RiskAnalysisResultRepository riskAnalysisResultRepository;



    private final V2XCommunicationLogRepository v2xCommunicationLogRepository;



    public DashboardSummaryDTO getDashboardSummary() {

        List<String> monitoredIds = listMonitoredIntersectionIds();

        long totalDevices = monitoredIds.size();

        Map<String, RiskAnalysisResult> latestById = buildLatestRiskByIntersectionId();



        long abnormalDevices = 0;

        double scoreSum = 0.0;

        for (String id : monitoredIds) {

            RiskAnalysisResult row = latestById.get(id);

            if (row == null) {

                continue;

            }

            String level = MonitoringRiskLevel.normalize(row.getRiskLevel());

            if (!MonitoringRiskLevel.isNormalLatest(level)) {

                abnormalDevices++;

            }

            scoreSum += row.getTotalRiskScore() != null ? row.getTotalRiskScore().doubleValue() : 0.0;

        }



        long normalDevices = totalDevices - abnormalDevices;



        double abnormalRate =

                totalDevices == 0 ? 0.0 : Math.min(100.0, (abnormalDevices * 100.0) / totalDevices);

        abnormalRate = Math.round(abnormalRate * 100.0) / 100.0;



        BigDecimal averageRiskScore =

                totalDevices == 0

                        ? BigDecimal.ZERO

                        : BigDecimal.valueOf(scoreSum / totalDevices).setScale(2, RoundingMode.HALF_UP);



        return new DashboardSummaryDTO(

                totalDevices,

                normalDevices,

                abnormalDevices,

                abnormalRate,

                averageRiskScore);

    }



    /**

     * 관측 대상 교차로 ID 목록. intersection_info 우선, 비어 있으면 risk 테이블의 교차로 ID.

     */

    private List<String> listMonitoredIntersectionIds() {

        LinkedHashSet<String> ids = new LinkedHashSet<>();

        for (IntersectionInfo ii : intersectionInfoRepository.findAll()) {

            if (ii.getIntersectionId() != null && !ii.getIntersectionId().isBlank()) {

                ids.add(ii.getIntersectionId().trim());

            }

        }

        if (!ids.isEmpty()) {

            return new ArrayList<>(ids);

        }

        for (String id : riskAnalysisResultRepository.findDistinctIntersectionIds()) {

            if (id != null && !id.isBlank()) {

                ids.add(id.trim());

            }

        }

        return new ArrayList<>(ids);

    }



    private Map<String, RiskAnalysisResult> buildLatestRiskByIntersectionId() {

        Map<String, RiskAnalysisResult> map = new HashMap<>();

        for (RiskAnalysisResult r : riskAnalysisResultRepository.findLatestRowPerIntersection()) {

            if (r.getIntersectionId() != null && !r.getIntersectionId().isBlank()) {

                map.put(r.getIntersectionId().trim(), r);

            }

        }

        return map;

    }



    public List<AbnormalDeviceDTO> getAbnormalDeviceList() {

        List<RiskAnalysisResult> abnormalList =

                riskAnalysisResultRepository.findAbnormalByNewestFirst(

                        MonitoringRiskLevel.LABEL_NORMAL, PageRequest.of(0, 5));



        return abnormalList.stream()

                .map(

                        result ->

                                new AbnormalDeviceDTO(

                                        result.getIntersectionId(),

                                        "DEVICE-" + result.getIntersectionId(),

                                        result.getTotalRiskScore(),

                                        result.getRiskLevel(),

                                        result.getAnalysisTime()))

                .collect(Collectors.toList());

    }



    public SpatLatencyDTO getSpatLatencyData(String intersectionId) {



        String normalizedIntersectionId =
                intersectionId == null ? "" : intersectionId.trim();
        boolean hasIntersectionFilter = !normalizedIntersectionId.isBlank();

        // 1. 최신 데이터 (필터가 있으면 해당 교차로 기준)
        V2XCommunicationLog latest =
                hasIntersectionFilter
                        ? v2xCommunicationLogRepository
                                .findTopByIntersectionIdOrderByV2xLogTimeDesc(normalizedIntersectionId)
                                .orElse(null)
                        : v2xCommunicationLogRepository.findTopByOrderByV2xLogTimeDesc().orElse(null);



        // 데이터 없을 경우

        if (latest == null) {

            return SpatLatencyDTO.builder()

                    .spatSuccessRate(0.0)

                    .status("데이터 없음")

                    .currentLatency(0.0)

                    .latencyTrend(List.of())

                    .currentSignal("-")

                    .remainTimeSeconds(0)

                    .packetStatus("-")

                    .build();

        }



        // 2. 성공률 계산 (전송 건수 대비 실패가 아닌 비율)

        int send = latest.getSpatSendCount();

        int fail = latest.getSpatFailCount();



        double successRate =

                send == 0

                        ? 0.0

                        : Math.max(0.0, Math.min(100.0, (send - fail) * 100.0 / send));



        // 3. 현재 latency

        double currentLatency = latest.getAvgLatencyMs().doubleValue();



        // 4. 상태 판단

        String status;

        if (currentLatency < 50) status = "우수";

        else if (currentLatency < 100) status = "보통";

        else status = "위험";



        // 5. 트렌드 (최근 10개)

        List<Double> latencyTrend =
                (hasIntersectionFilter
                                ? v2xCommunicationLogRepository
                                        .findTop10ByIntersectionIdOrderByV2xLogTimeDesc(
                                                normalizedIntersectionId)
                                : v2xCommunicationLogRepository.findTop10ByOrderByV2xLogTimeDesc())
                        .stream()
                        .map(log -> log.getAvgLatencyMs().doubleValue())
                        .toList();



        long nowSec = Instant.now().getEpochSecond();



        // 6. 반환 (신호·잔여시간은 조회 시각 기준으로 순환 — 대시보드 실시간 느낌)

        return SpatLatencyDTO.builder()

                .spatSuccessRate(successRate)

                .status(status)

                .currentLatency(currentLatency)

                .latencyTrend(latencyTrend)

                .currentSignal(deriveCurrentSignal(nowSec))

                .remainTimeSeconds(deriveRemainSeconds(nowSec))

                .packetStatus(derivePacketStatus(latest, successRate))

                .build();

    }



    /** 최신 로그 시각(초) 기준으로 신호 단계 순환 — 대시보드 표시용 */

    private static String deriveCurrentSignal(long epochSec) {

        String[] phases = {"직진 녹색", "황색", "좌회전 녹색", "적색", "보행 녹색"};

        int idx = (int) ((epochSec / 18) % phases.length);

        return phases[idx];

    }



    /** 다음 단계까지 남은 시간(초) — 1~30초 순환 */

    private static int deriveRemainSeconds(long epochSec) {

        int r = 30 - (int) (epochSec % 30);

        return r == 0 ? 30 : r;

    }



    private static String derivePacketStatus(V2XCommunicationLog latest, double spatSuccessRatePercent) {

        int fail = latest.getSpatFailCount();

        int comm = latest.getCommFailCount();

        if (fail == 0 && comm == 0) {

            return "정상";

        }

        if (spatSuccessRatePercent >= 95.0 && comm <= 2) {

            return "정상";

        }

        if (spatSuccessRatePercent >= 85.0 && comm <= 5) {

            return "일부 지연";

        }

        return "손실 감지";

    }

}

