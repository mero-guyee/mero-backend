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

    // 여행 hard delete 시, 발자취 물리 삭제를 막는 soft delete된 사진을 직접 제거
    @Modifying
    @Query(value = "DELETE FROM photo WHERE deleted_at IS NOT NULL " +
            "AND footprint_id IN (SELECT id FROM footprint WHERE trip_id = :tripId)", nativeQuery = true)
    void deleteSoftDeletedByTripId(@Param("tripId") Long tripId);

}
