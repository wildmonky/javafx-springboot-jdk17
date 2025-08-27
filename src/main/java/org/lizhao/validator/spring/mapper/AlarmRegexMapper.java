package org.lizhao.validator.spring.mapper;

import org.lizhao.validator.spring.entity.AlarmRegex;

import java.util.ArrayList;
import java.util.List;

public interface AlarmRegexMapper extends CommonMapper<AlarmRegex>{

    List<AlarmRegex> selectUnderDeviceInfoRule(String deviceId);

    List<AlarmRegex> selectUnderDeviceInfoType(String deviceId);

    List<AlarmRegex> selectUnderDevice(String deviceId);

    default List<AlarmRegex> selectByDeviceId(String deviceId) {
        List<AlarmRegex> reList = new ArrayList<>();
        reList.addAll(selectUnderDevice(deviceId));
        reList.addAll(selectUnderDeviceInfoType(deviceId));
        reList.addAll(selectUnderDeviceInfoRule(deviceId));
        return reList;
    }

}
