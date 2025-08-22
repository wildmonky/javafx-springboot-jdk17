package org.lizhao.validator.mapper;

import org.lizhao.validator.entity.AlarmRule;

import java.util.List;

public interface AlarmRuleMapper extends CommonMapper<AlarmRule> {

    List<AlarmRule> selectByDevice(String deviceId);

}
