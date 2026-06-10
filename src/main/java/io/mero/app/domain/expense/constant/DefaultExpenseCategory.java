package io.mero.app.domain.expense.constant;

public enum DefaultExpenseCategory {
    TRANSPORTATION("교통", "🚕", "#4CAF50"),
    FOOD("식비", "🍽️", "#FF9800"),
    ACCOMMODATION("숙박", "🏨", "#2196F3"),
    ACTIVITY("액티비티", "🎭", "#9C27B0"),
    SHOPPING("쇼핑", "🛍️", "#E91E63"),
    ETC("기타", "💰", "#607D8B");

    private final String name;
    private final String icon;
    private final String color;

    DefaultExpenseCategory(String name, String icon, String color) {
        this.name = name;
        this.icon = icon;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public String getColor() {
        return color;
    }
}
