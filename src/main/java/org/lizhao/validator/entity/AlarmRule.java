package org.lizhao.validator.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@TableName("DEVICE_SCADA_INFO_RULE")
public class AlarmRule {

    @TableId
    private String id;

    /**
     * {@link AlarmType#getId()}
     */
    @TableField("DEVICE_SCADA_INFO_TYPE_ID")
    private String deviceScadaInfoTypeId;

    @TableField("NAME")
    private String name;

    @TableField("RULE")
    private String rule;

    // 正则表达式
    @TableField("PATTERN_ID")
    private String patternId;

    @TableField("INCLUDE_DEVICE_NAME")
    private Boolean includeDeviceName;

    @TableField("FULL_TEXT_MATCH")
    private Boolean fullTextMatch;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlarmRule)) return false;
        AlarmRule that = (AlarmRule) o;
        return Objects.equals(id, that.id) && Objects.equals(deviceScadaInfoTypeId, that.deviceScadaInfoTypeId) && Objects.equals(name, that.name) && Objects.equals(rule, that.rule) && Objects.equals(patternId, that.patternId) && Objects.equals(includeDeviceName, that.includeDeviceName) && Objects.equals(fullTextMatch, that.fullTextMatch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, deviceScadaInfoTypeId, name, rule, patternId, includeDeviceName, fullTextMatch);
    }
}
