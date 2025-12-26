package io.mero.app.domain.expense.repository;

import io.mero.app.domain.diary.entity.Diary;
import io.mero.app.domain.expense.entity.Expense;
import io.mero.app.domain.expense.entity.ExpenseCategory;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByTripOrderByDateDesc(Trip trip);

    List<Expense> findByDiary(Diary diary);

    List<Expense> findByCategory(ExpenseCategory category);

}