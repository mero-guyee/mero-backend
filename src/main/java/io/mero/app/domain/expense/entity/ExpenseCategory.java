package io.mero.app.domain.expense.entity;

import io.mero.app.domain.user.entity.User;
import io.mero.app.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "expense_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 50)
    private String icon;

    @Column(length = 20)
    private String color;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Builder
    public ExpenseCategory(User user, String name, String icon, String color,
                           Boolean isDefault, Integer displayOrder) {
        this.user = user;
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.isDefault = isDefault != null ? isDefault : false;
        this.displayOrder = displayOrder;
    }

    public void updateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("카테고리 이름은 필수입니다");
        }
        this.name = name;
    }

    public void updateIcon(String icon) {
        this.icon = icon;
    }

    public void updateColor(String color) {
        this.color = color;
    }

    public void updateDisplayOrder(Integer displayOrder) {
        if (displayOrder == null || displayOrder < 0) {
            throw new IllegalArgumentException("순서는 0 이상이어야 합니다");
        }
        this.displayOrder = displayOrder;
    }

    public boolean canDelete() {
        return !this.isDefault;
    }
}