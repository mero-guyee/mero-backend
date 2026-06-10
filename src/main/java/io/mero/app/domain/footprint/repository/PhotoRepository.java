package io.mero.app.domain.footprint.repository;

import io.mero.app.domain.footprint.entity.Footprint;
import io.mero.app.domain.footprint.entity.Photo;
import io.mero.app.domain.footprint.entity.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {

    List<Photo> findByFootprintOrderByOrderIndexAsc(Footprint footprint);

    long countByFootprint(Footprint footprint);

    Optional<Photo> findByClientId(String clientId);

}
