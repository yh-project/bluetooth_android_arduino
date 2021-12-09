package com.example.btex;

import androidx.annotation.Nullable;

public class LogData {
    public String content;
    public String date;
    public int type;

    public LogData(@Nullable String content, @Nullable String date, int type) {
        this.content = content;
        this.date = date;
        this.type = type;
    }
}
