package com.neilldev.nest.Data;

import java.io.Serializable;
import java.util.Objects;

public class TempDataObj implements Serializable {
    public String temp;
    public String timestamp;

    public TempDataObj(String temp, String timestamp) {
        this.temp = temp;
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TempDataObj that = (TempDataObj) o;

        if (!Objects.equals(temp, that.temp)) return false;
        return Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        int result = temp != null ? temp.hashCode() : 0;
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }
}
