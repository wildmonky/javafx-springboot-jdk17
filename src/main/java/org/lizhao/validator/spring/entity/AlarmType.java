package org.lizhao.validator.spring.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@TableName("DEVICE_SCADA_INFO_TYPE")
public class AlarmType {

    @TableId("ID")
    private String id;

    /**
     * 设备Id {@link Device#getId()}
     */
    @TableField("DEVICE_ID")
    private String deviceId;

    @TableField("INCLUDE_DEVICE_NAME")
    private Boolean includeDeviceName;

    @TableField("FULL_TEXT_MATCH")
    private Boolean fullTextMatch;

    @TableField("NAME")
    private String name;

    /**
     * 是否需要上传
     * "1"--是，上传
     * "0"--否，不上传
     */
    @TableField("UPLOAD")
    private Boolean upload;

    /**
     * 是否需要主站合并
     * "1"--是，上传
     * "0"--否，不上传
     */
    @TableField("MASTER_MERGE")
    private Boolean masterMerge;

    // 优先级
    @TableField("PRIORITY")
    private Integer priority;

    @TableField("PATTERN_ID")
    private String patternId;

    // 备注
    @TableField("\"COMMENT\"")
    private String comment;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlarmType)) return false;
        AlarmType infoType = (AlarmType) o;
        return Objects.equals(id, infoType.id) && Objects.equals(deviceId, infoType.deviceId) && Objects.equals(includeDeviceName, infoType.includeDeviceName) && Objects.equals(fullTextMatch, infoType.fullTextMatch) && Objects.equals(name, infoType.name) && Objects.equals(upload, infoType.upload) && Objects.equals(masterMerge, infoType.masterMerge) && Objects.equals(priority, infoType.priority) && Objects.equals(patternId, infoType.patternId) && Objects.equals(comment, infoType.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, deviceId, includeDeviceName, fullTextMatch, name, upload, masterMerge, priority, patternId, comment);
    }
}
