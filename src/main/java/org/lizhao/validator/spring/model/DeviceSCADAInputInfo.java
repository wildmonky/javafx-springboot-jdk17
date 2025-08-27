package org.lizhao.validator.spring.model;

import lombok.Getter;
import lombok.Setter;
import org.lizhao.validator.spring.entity.Device;

import java.util.List;


/**
 * 设备警告信号 excel导入数据转换
 */
@Getter
@Setter
public class DeviceSCADAInputInfo {

    private String pointCode;

    private String device;

    private String warnInfo;

    private String voltage;

    //------------------校验用------------------------------
    private List<Device> devices;

    // 解析后的信息，排除了电压、设备名
    private String info;

    private Boolean voltageCheckResult;

    private Boolean devicesCheckResult;

//    // 匹配上 信息类型
//    private Boolean matchType;

    // 输入信息对应的信息类型id 用于检测是否需要合并
    private String infoTypeId;

    private String infoTypeName;

    // 匹配方式 true-名称;false|null-pattern
    private Boolean matchType;

    // 对应设备类型 是否主站合并
    private Boolean masterMerge;

    // 检测信息列表，检测完后拼接输出
    private List<String> tips;

    // 耗时
    private long cost;

    // 适配了多少规则
    private long ruleCount;

}
