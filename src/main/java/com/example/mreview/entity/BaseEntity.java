package com.example.mreview.entity;

import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import java.time.LocalDateTime;

@MappedSuperclass  // 이 어노테이션이 적용되면 테이블을 따로 만들지 않음, 이 클래스를 상송한 클래스만 테이블을 생성할 수 있음.
@EntityListeners(value = { AuditingEntityListener.class}) // JPA 내부에서 엔티티 객체가 생성/변경되는 것을 감지하는 역할을 하는 리스너, main메소드가 있는 클래스에 @EnableJpaAuditing 어노테이션 추가 필요
@Getter
abstract public class BaseEntity {

    @CreatedDate
    @Column(name = "regdate", updatable = false)
    private LocalDateTime regDate;

    @LastModifiedDate
    @Column(name = "moddate")
    private LocalDateTime modDate;


}
