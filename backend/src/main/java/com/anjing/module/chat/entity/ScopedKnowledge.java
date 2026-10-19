package com.anjing.module.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="cs_scoped_knowledge") @Getter @Setter
public class ScopedKnowledge {
    @Id @Column(length=64) private String id;
    @Column(nullable=false,length=64) private String merchantId;
    @Column(length=64) private String shopId;
    @Column(nullable=false,length=20) private String scope;
    @Column(nullable=false,length=64) private String topic;
    @Column(nullable=false) private String title;
    @Column(nullable=false,columnDefinition="TEXT") private String content;
    @Column(nullable=false,length=1000) private String source;
    @Column(nullable=false,length=20) private String status;
    @Column(nullable=false) private boolean demoData;
}
