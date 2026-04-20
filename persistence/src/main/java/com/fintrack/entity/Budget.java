package com.fintrack.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;


@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {
                "user_id",
                "category_id",
                "month",
                "year"
        })
})
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal monthlyLimit;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Budget(BigDecimal monthlyLimit, Integer month, Integer year, Category category, User user) {
        this.monthlyLimit = monthlyLimit;
        this.month = month;
        this.year = year;
        this.category = category;
        this.user = user;
    }

    @Override
    public String toString() {
        return """
                Budget {id=%d, monthlyLimit=%s, month=%s, year=%s}
                """.formatted(id, monthlyLimit, month, year);
    }

}
