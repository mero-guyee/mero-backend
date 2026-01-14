package io.mero.app.domain.expense.service;

import io.mero.app.domain.expense.constant.DefaultExpenseCategory;
import io.mero.app.domain.expense.dto.ExpenseCategoryCreateRequest;
import io.mero.app.domain.expense.entity.Expense;
import io.mero.app.domain.expense.entity.ExpenseCategory;
import io.mero.app.domain.expense.repository.ExpenseCategoryRepository;
import io.mero.app.domain.expense.repository.ExpenseRepository;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.user.entity.User;
import io.mero.app.domain.user.repository.UserRepository;
import io.mero.app.global.enums.Currency;
import io.mero.app.global.exception.BadRequestException;
import io.mero.app.global.exception.DuplicateException;
import io.mero.app.global.exception.ForbiddenException;
import io.mero.app.global.exception.NotFoundException;
import io.mero.app.global.util.MessageUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExpenseCategoryServiceTest {

    @Mock
    private ExpenseCategoryRepository expenseCategoryRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageUtil messageUtil;

    @InjectMocks
    private ExpenseCategoryService expenseCategoryService;

    @Test
    @DisplayName("기본 카테고리 생성 성공")
    void createDefaultCategoriesForUser_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        // when
        expenseCategoryService.createDefaultCategoriesForUser(user);

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExpenseCategory>> captor = ArgumentCaptor.forClass(List.class);
        verify(expenseCategoryRepository).saveAll(captor.capture());

        List<ExpenseCategory> savedCategories = captor.getValue();
        assertThat(savedCategories).hasSize(DefaultExpenseCategory.values().length);

        // 기타 카테고리가 포함되어 있는지 확인
        boolean hasEtcCategory = savedCategories.stream()
                .anyMatch(c -> c.getName().equals(DefaultExpenseCategory.ETC.getName()));
        assertThat(hasEtcCategory).isTrue();

        // 모든 카테고리가 isDefault=true인지 확인
        assertThat(savedCategories).allMatch(ExpenseCategory::getIsDefault);
    }

    @Test
    @DisplayName("카테고리 생성 성공")
    void createExpenseCategory_Success() {
        // given
        Long userId = 1L;
        User user = User.builder().id(userId).build();
        ExpenseCategoryCreateRequest request = new ExpenseCategoryCreateRequest(
                userId, "새 카테고리", "🎯", "#FF0000"
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(expenseCategoryRepository.existsByUserAndName(user, request.getName())).willReturn(false);
        given(expenseCategoryRepository.countByUser(user)).willReturn(6);

        // when
        expenseCategoryService.createExpenseCategory(userId, request);

        // then
        ArgumentCaptor<ExpenseCategory> captor = ArgumentCaptor.forClass(ExpenseCategory.class);
        verify(expenseCategoryRepository).save(captor.capture());

        ExpenseCategory savedCategory = captor.getValue();
        assertThat(savedCategory.getName()).isEqualTo("새 카테고리");
        assertThat(savedCategory.getIcon()).isEqualTo("🎯");
        assertThat(savedCategory.getColor()).isEqualTo("#FF0000");
        assertThat(savedCategory.getIsDefault()).isFalse();
        assertThat(savedCategory.getDisplayOrder()).isEqualTo(6);
    }

    @Test
    @DisplayName("카테고리 생성 실패 - 중복된 카테고리 이름")
    void createExpenseCategory_Fail_DuplicateName() {
        // given
        Long userId = 1L;
        User user = User.builder().id(userId).build();
        ExpenseCategoryCreateRequest request = new ExpenseCategoryCreateRequest(
                userId, "식비", "🍔", "#FF0000"
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(expenseCategoryRepository.existsByUserAndName(user, request.getName())).willReturn(true);
        given(messageUtil.getMessage("error.expenseCategory.duplicateName"))
                .willReturn("이미 존재하는 카테고리 이름입니다");

        // when & then
        assertThatThrownBy(() -> expenseCategoryService.createExpenseCategory(userId, request))
                .isInstanceOf(DuplicateException.class)
                .hasMessage("이미 존재하는 카테고리 이름입니다");
    }

    @Test
    @DisplayName("카테고리 생성 실패 - 사용자를 찾을 수 없음")
    void createExpenseCategory_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        ExpenseCategoryCreateRequest request = new ExpenseCategoryCreateRequest(
                userId, "새 카테고리", "🎯", "#FF0000"
        );

        given(userRepository.findById(userId)).willReturn(Optional.empty());
        given(messageUtil.getMessage("error.user.notFound"))
                .willReturn("사용자를 찾을 수 없습니다");

        // when & then
        assertThatThrownBy(() -> expenseCategoryService.createExpenseCategory(userId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("카테고리 삭제 성공 - 해당 카테고리의 지출이 기타 카테고리로 변경됨")
    void deleteCategory_Success_ExpensesMovedToEtcCategory() {
        // given
        Long userId = 1L;
        Long categoryId = 1L;

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(1L)
                .user(user)
                .defaultCurrency(Currency.KRW)
                .build();

        ExpenseCategory categoryToDelete = ExpenseCategory.builder()
                .user(user)
                .name("삭제할 카테고리")
                .icon("🎯")
                .color("#FF0000")
                .isDefault(false)
                .displayOrder(10)
                .build();

        ExpenseCategory etcCategory = ExpenseCategory.builder()
                .user(user)
                .name(DefaultExpenseCategory.ETC.getName())
                .icon(DefaultExpenseCategory.ETC.getIcon())
                .color(DefaultExpenseCategory.ETC.getColor())
                .isDefault(true)
                .displayOrder(5)
                .build();

        Expense expense1 = Expense.builder()
                .id(1L)
                .trip(trip)
                .amount(new BigDecimal("100"))
                .currency(Currency.KRW)
                .category(categoryToDelete)
                .date(LocalDate.now())
                .build();

        Expense expense2 = Expense.builder()
                .id(2L)
                .trip(trip)
                .amount(new BigDecimal("200"))
                .currency(Currency.KRW)
                .category(categoryToDelete)
                .date(LocalDate.now())
                .build();

        given(expenseCategoryRepository.findById(categoryId)).willReturn(Optional.of(categoryToDelete));
        given(expenseCategoryRepository.findByUserAndName(user, DefaultExpenseCategory.ETC.getName()))
                .willReturn(Optional.of(etcCategory));
        given(expenseRepository.findByCategory(categoryToDelete)).willReturn(List.of(expense1, expense2));

        // when
        expenseCategoryService.deleteCategory(userId, categoryId);

        // then
        assertThat(expense1.getCategory()).isEqualTo(etcCategory);
        assertThat(expense2.getCategory()).isEqualTo(etcCategory);
        verify(expenseCategoryRepository).delete(categoryToDelete);
    }

    @Test
    @DisplayName("카테고리 삭제 성공 - 해당 카테고리를 사용하는 지출이 없는 경우")
    void deleteCategory_Success_NoExpenses() {
        // given
        Long userId = 1L;
        Long categoryId = 1L;

        User user = User.builder().id(userId).build();

        ExpenseCategory categoryToDelete = ExpenseCategory.builder()
                .user(user)
                .name("삭제할 카테고리")
                .isDefault(false)
                .displayOrder(10)
                .build();

        ExpenseCategory etcCategory = ExpenseCategory.builder()
                .user(user)
                .name(DefaultExpenseCategory.ETC.getName())
                .isDefault(true)
                .displayOrder(5)
                .build();

        given(expenseCategoryRepository.findById(categoryId)).willReturn(Optional.of(categoryToDelete));
        given(expenseCategoryRepository.findByUserAndName(user, DefaultExpenseCategory.ETC.getName()))
                .willReturn(Optional.of(etcCategory));
        given(expenseRepository.findByCategory(categoryToDelete)).willReturn(List.of());

        // when
        expenseCategoryService.deleteCategory(userId, categoryId);

        // then
        verify(expenseCategoryRepository).delete(categoryToDelete);
    }

    @Test
    @DisplayName("카테고리 삭제 실패 - 기본 카테고리는 삭제 불가")
    void deleteCategory_Fail_CannotDeleteDefaultCategory() {
        // given
        Long userId = 1L;
        Long categoryId = 1L;

        User user = User.builder().id(userId).build();

        ExpenseCategory defaultCategory = ExpenseCategory.builder()
                .user(user)
                .name("기본 카테고리")
                .isDefault(true)  // 기본 카테고리
                .displayOrder(1)
                .build();

        given(expenseCategoryRepository.findById(categoryId)).willReturn(Optional.of(defaultCategory));
        given(messageUtil.getMessage("error.expenseCategory.cannotDeleteDefault"))
                .willReturn("기본 카테고리는 삭제할 수 없습니다");

        // when & then
        assertThatThrownBy(() -> expenseCategoryService.deleteCategory(userId, categoryId))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("기본 카테고리는 삭제할 수 없습니다");
    }

    @Test
    @DisplayName("카테고리 삭제 실패 - 다른 사용자의 카테고리")
    void deleteCategory_Fail_NotOwner() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long categoryId = 1L;

        User otherUser = User.builder().id(otherUserId).build();

        ExpenseCategory otherUserCategory = ExpenseCategory.builder()
                .user(otherUser)
                .name("다른 사용자의 카테고리")
                .isDefault(false)
                .displayOrder(1)
                .build();

        given(expenseCategoryRepository.findById(categoryId)).willReturn(Optional.of(otherUserCategory));
        given(messageUtil.getMessage("error.forbidden")).willReturn("접근 권한이 없습니다");

        // when & then
        assertThatThrownBy(() -> expenseCategoryService.deleteCategory(userId, categoryId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("접근 권한이 없습니다");
    }

    @Test
    @DisplayName("카테고리 삭제 실패 - 카테고리를 찾을 수 없음")
    void deleteCategory_Fail_CategoryNotFound() {
        // given
        Long userId = 1L;
        Long categoryId = 999L;

        given(expenseCategoryRepository.findById(categoryId)).willReturn(Optional.empty());
        given(messageUtil.getMessage("error.expenseCategory.notFound"))
                .willReturn("지출 카테고리를 찾을 수 없습니다");

        // when & then
        assertThatThrownBy(() -> expenseCategoryService.deleteCategory(userId, categoryId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("지출 카테고리를 찾을 수 없습니다");
    }
}
