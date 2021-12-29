package com.neilldev.nest;

import java.io.Serializable;

public class TempDataObj implements Serializable {
    public String temp;
    public String timestamp;

    public TempDataObj(String temp, String timestamp) {
        this.temp = temp;
        this.timestamp = timestamp;
    }
}
