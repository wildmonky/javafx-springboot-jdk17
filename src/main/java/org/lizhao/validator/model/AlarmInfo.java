package org.lizhao.validator.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AlarmInfo {

    private String rowNum;

    private String pointCode;

    private String alarmContent;

    private String device;

    private Boolean needReport;

    private Boolean needMainMerge;

    private String message;

}
