package io.mero.app.domain.expense.repository;

import io.mero.app.domain.expense.entity.ExpenseCategory;
import io.mero.app.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    List<ExpenseCategory> findByUserOrderByDisplayOrderAsc(User user);

    boolean existsByUserAndName(User user, String name);

    Optional<ExpenseCategory> findByUserAndName(User user, String name);

    int countByUser(User user);

}