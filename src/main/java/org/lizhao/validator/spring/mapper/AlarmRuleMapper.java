package org.lizhao.validator.spring.mapper;

import org.lizhao.validator.spring.entity.AlarmRule;

import java.util.List;

public interface AlarmRuleMapper extends CommonMapper<AlarmRule> {

    List<AlarmRule> selectByDevice(String deviceId);

}
