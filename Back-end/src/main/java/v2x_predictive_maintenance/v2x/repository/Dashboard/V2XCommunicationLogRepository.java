package v2x_predictive_maintenance.v2x.repository.Dashboard;

import org.springframework.data.jpa.repository.JpaRepository;
import v2x_predictive_maintenance.v2x.entity.Dashboard.V2XCommunicationLog;

import java.util.List;
import java.util.Optional;

public interface V2XCommunicationLogRepository extends JpaRepository<V2XCommunicationLog, Long> {

    // 최신 데이터 1개 (현재 상태)
    Optional<V2XCommunicationLog> findTopByOrderByV2xLogTimeDesc();

    // 최신 10개 (그래프용)
    List<V2XCommunicationLog> findTop10ByOrderByV2xLogTimeDesc();

    // 특정 교차로의 최신 V2X 상태 1개 조회
    Optional<V2XCommunicationLog>
    findTopByIntersectionIdOrderByV2xLogTimeDesc(String intersectionId);

    // 특정 교차로 그래프용 최신 10개
    List<V2XCommunicationLog>
    findTop10ByIntersectionIdOrderByV2xLogTimeDesc(String intersectionId);

    // 최근 1시간 지연 그래프용 최신 6개
    List<V2XCommunicationLog>
    findTop6ByIntersectionIdOrderByV2xLogTimeDesc(String intersectionId);

    // 특정 교차로 전체 로그 최신순 (테이블용)
    List<V2XCommunicationLog>
    findByIntersectionIdOrderByV2xLogTimeDesc(String intersectionId);

}