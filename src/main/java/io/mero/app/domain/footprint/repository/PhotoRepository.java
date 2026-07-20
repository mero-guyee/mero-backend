package io.mero.app.domain.footprint.repository;

import io.mero.app.domain.footprint.entity.Footprint;
import io.mero.app.domain.footprint.entity.Photo;
import io.mero.app.domain.footprint.entity.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {

    List<Photo> findByFootprintOrderByOrderIndexAsc(Footprint footprint);

    long countByFootprint(Footprint footprint);

    // soft delete(@SQLRestriction)를 무시하고 삭제된 행까지 조회 (clientId 재사용/복구용)
    @Query(value = "SELECT * FROM photo WHERE client_id = :clientId LIMIT 1", nativeQuery = true)
    Optional<Photo> findByClientIdIncludingDeleted(@Param("clientId") String clientId);

    // 활성 발자취에 속한 soft delete된 사진 조회 (bulk delete 전 스토리지 정리용)
    // 삭제된 발자취의 사진은 findByDeletedFootprintTripId가 처리하므로 제외한다.
    @Query(value = "SELECT * FROM photo WHERE deleted_at IS NOT NULL " +
            "AND footprint_id IN (SELECT id FROM footprint WHERE trip_id = :tripId AND deleted_at IS NULL)",
            nativeQuery = true)
    List<Photo> findSoftDeletedByTripId(@Param("tripId") Long tripId);

    // 활성 발자취의 물리 삭제(JPA cascade)를 막는 soft delete된 사진을 직접 제거
    @Modifying
    @Query(value = "DELETE FROM photo WHERE deleted_at IS NOT NULL " +
            "AND footprint_id IN (SELECT id FROM footprint WHERE trip_id = :tripId AND deleted_at IS NULL)",
            nativeQuery = true)
    void deleteSoftDeletedByTripId(@Param("tripId") Long tripId);

    // soft delete된 발자취에 매달린 사진 조회 (발자취 hard delete 전 스토리지 정리용)
    @Query(value = "SELECT * FROM photo WHERE footprint_id IN " +
            "(SELECT id FROM footprint WHERE trip_id = :tripId AND deleted_at IS NOT NULL)", nativeQuery = true)
    List<Photo> findByDeletedFootprintTripId(@Param("tripId") Long tripId);

    // soft delete된 발자취의 사진을 직접 제거 (발자취 행 삭제 시 FK 위반 방지, soft delete 여부 무관)
    @Modifying
    @Query(value = "DELETE FROM photo WHERE footprint_id IN " +
            "(SELECT id FROM footprint WHERE trip_id = :tripId AND deleted_at IS NOT NULL)", nativeQuery = true)
    void deleteByDeletedFootprintTripId(@Param("tripId") Long tripId);

}
