package io.mero.app.domain.expense.service;

import io.mero.app.domain.expense.constant.DefaultExpenseCategory;
import io.mero.app.domain.expense.dto.ExpenseCategoryCreateRequest;
import io.mero.app.domain.expense.dto.ExpenseCategoryResponse;
import io.mero.app.domain.expense.entity.Expense;
import io.mero.app.domain.expense.entity.ExpenseCategory;
import io.mero.app.domain.expense.repository.ExpenseCategoryRepository;
import io.mero.app.domain.expense.repository.ExpenseRepository;
import io.mero.app.domain.user.entity.User;
import io.mero.app.domain.user.repository.UserRepository;
import io.mero.app.global.exception.BadRequestException;
import io.mero.app.global.exception.DuplicateException;
import io.mero.app.global.exception.ForbiddenException;
import io.mero.app.global.exception.NotFoundException;
import io.mero.app.global.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final MessageUtil messageUtil;

    @Transactional
    public void createDefaultCategoriesForUser(User user) {
        List<ExpenseCategory> defaultCategories = Arrays.stream(DefaultExpenseCategory.values())
                .map(defaultCategory -> ExpenseCategory.builder()
                        .user(user)
                        .name(defaultCategory.getName())
                        .icon(defaultCategory.getIcon())
                        .color(defaultCategory.getColor())
                        .isDefault(true)
                        .displayOrder(defaultCategory.ordinal())
                        .build())
                .toList();

        expenseCategoryRepository.saveAll(defaultCategories);
    }

    public List<ExpenseCategoryResponse> getCategories(Long userId) {
        User user = findUserById(userId);
        List<ExpenseCategory> expenseCategories = expenseCategoryRepository.findByUserOrderByDisplayOrderAsc(user);
        return expenseCategories.stream()
                .map(ExpenseCategoryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void createExpenseCategory(Long userId, ExpenseCategoryCreateRequest request) {
        // 멱등성 체크: 동일한 clientId로 이미 생성된 ExpenseCategory가 있으면 리턴
        if (expenseCategoryRepository.findByClientIdAndUserId(request.getClientId(), userId).isPresent()) {
            return;
        }

        User user = findUserById(userId);

        validateDuplicateCategoryName(user, request.getName());

        int displayOrder = expenseCategoryRepository.countByUser(user);

        ExpenseCategory category = ExpenseCategory.builder()
                .user(user)
                .clientId(request.getClientId())
                .name(request.getName())
                .icon(request.getIcon())
                .color(request.getColor())
                .isDefault(false)
                .displayOrder(displayOrder)
                .build();

        expenseCategoryRepository.save(category);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        messageUtil.getMessage("error.user.notFound")));
    }

    private void validateDuplicateCategoryName(User user, String name) {
        if (expenseCategoryRepository.existsByUserAndName(user, name)) {
            throw new DuplicateException(
                    messageUtil.getMessage("error.expenseCategory.duplicateName"));
        }
    }

    @Transactional
    public void deleteCategory(Long userId, Long categoryId) {
        ExpenseCategory category = findCategoryById(categoryId);
        validateCategoryOwner(category, userId);

        if (!category.canDelete()) {
            throw new BadRequestException(
                    messageUtil.getMessage("error.expenseCategory.cannotDeleteDefault"));
        }

        ExpenseCategory defaultCategory = findDefaultCategoryEtc(category.getUser());

        List<Expense> expenses = expenseRepository.findByCategory(category);
        for (Expense expense : expenses) {
            expense.changeCategory(defaultCategory);
        }

        category.delete();
    }

    private ExpenseCategory findCategoryById(Long categoryId) {
        return expenseCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(
                        messageUtil.getMessage("error.expenseCategory.notFound")));
    }

    private ExpenseCategory findDefaultCategoryEtc(User user) {
        String etcCategoryName = DefaultExpenseCategory.ETC.getName();
        return expenseCategoryRepository.findByUserAndName(user, etcCategoryName)
                .orElseThrow(() -> new NotFoundException(
                        messageUtil.getMessage("error.expenseCategory.defaultNotFound")));
    }

    private void validateCategoryOwner(ExpenseCategory category, Long userId) {
        if (!category.getUser().getId().equals(userId)) {
            throw new ForbiddenException(
                    messageUtil.getMessage("error.forbidden"));
        }
    }
}
