package com.ajemi.barber.Ta7li9_app.dto;

import lombok.Data;

@Data
public class QueueInfoDto {
    private int waitTime;
    private int queuePosition;

    public QueueInfoDto(int waitTime, int queuePosition) {
        this.waitTime = waitTime;
        this.queuePosition = queuePosition;
    }
}
