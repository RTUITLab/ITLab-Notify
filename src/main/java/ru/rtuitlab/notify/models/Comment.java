package ru.rtuitlab.notify.models;

import lombok.Data;

@Data
public class Comment {
    private String sender;
    private String date;
    private String report;
    private String user;
}
