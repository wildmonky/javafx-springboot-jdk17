package org.lizhao.validator.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@Getter
@Setter
@ToString
@TableName("\"DEVICE\"")
public class Device {

    @TableId("ID")
    private String id;

    @TableField("CHILD_ID")
    private String childId;

    @TableField("NAME")
    private String name;

    // 根据电压、设备类型查询对应设备
    @TableField("VOLTAGE")
    private String voltage;

    @TableField("TYPE")
    private String type;

    @TableField("PATTERN_ID")
    private String patternId;

    @TableField(exist = false)
    private Boolean checkResult;

    @TableField(exist = false)
    private String checkResultMessage;

    public Device() {}

    public Device(String voltage, String name, String type) {
        this.voltage = voltage;
        this.name = name;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Device device = (Device) o;
        return Objects.equals(id, device.id) && Objects.equals(childId, device.childId) && Objects.equals(name, device.name) && Objects.equals(voltage, device.voltage) && Objects.equals(type, device.type) && Objects.equals(patternId, device.patternId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, childId, name, voltage, type, patternId);
    }
}
