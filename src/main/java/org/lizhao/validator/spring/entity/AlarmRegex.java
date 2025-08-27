package org.lizhao.validator.spring.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@TableName("DEVICE_SCADA_REGEX")
public class AlarmRegex {

    @TableId("ID")
    private String id;

    @TableField("PATTERN")
    private String pattern;

    @TableField("NOT_MATCH_MESSAGE")
    private String notMatchMessage;

}
