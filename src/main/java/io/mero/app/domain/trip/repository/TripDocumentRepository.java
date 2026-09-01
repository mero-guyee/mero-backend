package io.mero.app.domain.trip.repository;

import io.mero.app.domain.trip.entity.TripDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripDocumentRepository extends JpaRepository<TripDocument, Long> {
    List<TripDocument> findByTripId(Long tripId);

    // soft delete(@SQLRestriction)를 무시하고 삭제된 행까지 조회 (clientId 재사용/복구용)
    @Query(value = "SELECT * FROM trip_documents WHERE client_id = :clientId AND trip_id = :tripId LIMIT 1",
            nativeQuery = true)
    Optional<TripDocument> findByClientIdAndTripIdIncludingDeleted(@Param("clientId") String clientId,
                                                                   @Param("tripId") Long tripId);

    // 삭제된 문서까지 포함해 스토리지 정리 대상 조회 (여행 hard delete 시 고아 파일 방지).
    // 엔티티가 아니라 키만 조회한다: 곧 bulk delete로 사라질 행을 영속성 컨텍스트에 올리면
    // 이후 flush에서 detach된 trip 참조를 transient로 오인해 TransientObjectException이 난다.
    @Query(value = "SELECT stored_file_name FROM trip_documents WHERE trip_id = :tripId", nativeQuery = true)
    List<String> findStorageKeysByTripId(@Param("tripId") Long tripId);

    // 여행 hard delete 시, cascade로 정리되지 않는 soft delete된 문서를 직접 제거
    @Modifying
    @Query(value = "DELETE FROM trip_documents WHERE trip_id = :tripId AND deleted_at IS NOT NULL", nativeQuery = true)
    void deleteSoftDeletedByTripId(@Param("tripId") Long tripId);
}
