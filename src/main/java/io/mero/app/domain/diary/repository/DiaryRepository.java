package io.mero.app.domain.diary.repository;

import io.mero.app.domain.diary.entity.Diary;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    List<Diary> findByTripIdOrderByDateDesc(Long tripId);

}
