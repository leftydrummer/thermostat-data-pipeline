package com.neilldev.nest.Data;

import java.io.Serializable;
import java.util.Objects;

public class HumidDataObj implements Serializable {
    public String humidity_percent;
    public String timestamp;

    public HumidDataObj(String humidity_percent, String timestamp) {
        this.humidity_percent = humidity_percent;
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HumidDataObj that = (HumidDataObj) o;

        if (!Objects.equals(humidity_percent, that.humidity_percent))
            return false;
        return Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        int result = humidity_percent != null ? humidity_percent.hashCode() : 0;
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }
}
