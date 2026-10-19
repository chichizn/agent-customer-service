package com.anjing.module.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name="cs_chat_execution", indexes=@Index(name="idx_chat_execution_session",columnList="sessionId"))
@Getter @Setter
public class ChatExecution {
    @Id @Column(length=64) private String id;
    @Column(nullable=false,length=64) private String sessionId;
    @Column(nullable=false,length=64) private String action;
    @Column(nullable=false,length=16) private String status;
    @Column(nullable=false,columnDefinition="TEXT") private String summary;
    @Column(length=64) private String relatedId;
    private LocalDateTime createdAt;
}
