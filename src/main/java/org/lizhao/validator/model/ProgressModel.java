package org.lizhao.validator.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ProgressModel {

    private long total;

    private long processed = 0;

    private long remain;

    public ProgressModel(long total) {
        this.total = total;
        this.remain = total;
    }

    public void setProcessed(long processed) {
        if (processed < this.processed || processed > this.total) {
            return;
        }

        this.processed = processed;
        this.remain = this.total - processed;
    }

    public double getProgress() {
        return processed * 1d / total;
    }
}
