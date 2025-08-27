package org.lizhao.validator.spring.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum DeviceTypeEnum {

    MAIN_TRANSFORMER("主变", "mainTransformer",true),
    STATION_POWER("站用电", "stationPower",true),
    DOUBLE_GENERATRIX("双母线", "doubleGeneratrix",true),
    GENERATRIX("母线", "generatrix",true),
    LINE("线路", "line",true),
    LINE_HIGH_RESISTANCE("线高抗", "lineHighResistance",true),
    CAPACITANCE_AND_REACTANCE("电容器组/电抗器组", "capacitanceAndReactance",true),
    STATION_TRANSFORMER("站用变", "stationTransformer",true),
    GROUND_TRANSFORMER("接地变", "groundTransformer",true),

    ZIGZAG_TRANSFORMER("曲折变", "zigzagTransformer",true),
    ZIGZAG_AND_GROUND_TRANSFORMER("曲折变/接地变", "zigzagAndGroundTransformer",true),
    SWITCH("开关间隔", "switch",true),
    PETERSON_COIL("消弧线圈", "petersonCoil",false),
    SMALL_RESISTOR("小电阻", "resistor",false),

    PUBLIC("公用", "public",false),
    DC_SYSTEM("直流系统", "dcSystem",false),
    STABILITY_CONTROL("稳控", "stabilityControl",false),
    BACKUP_AUTOMATIC_SWITCH("备自投装置", "backupAutomaticSwitch",false),
    OUT_OF_STEP_SEPARATION("失步解列", "outOfStepSeparation",false),
    FREQUENCY_VOLTAGE_EMERGENCY_CONTROL("频率电压紧急控制", "frequencyVoltageEmergencyControl",false),
    VQC("VQC", "vqc",false),
    COMMUNICATION_POWER_CONTROL("通信电源控制", "communicationPowerControl",false),
    SMALL_CURRENT("小电流", "smallCurrent",false);

    private final boolean needVoltage;
    private final String name;
    private final String desc;

    DeviceTypeEnum(String desc, String name, boolean needVoltage) {
        this.desc = desc;
        this.name = name;
        this.needVoltage = needVoltage;
    }

    public static DeviceTypeEnum of(String desc) {
        return Arrays.stream(values()).filter(e -> e.getDesc().equals(desc)).findFirst().orElse(null);
    }

}
