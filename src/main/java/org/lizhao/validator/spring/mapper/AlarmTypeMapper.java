package org.lizhao.validator.spring.mapper;

import org.lizhao.validator.spring.entity.AlarmType;
import org.lizhao.validator.spring.model.AlarmTypeRuleModel;

import java.util.List;
import java.util.Set;

public interface AlarmTypeMapper extends CommonMapper<AlarmType> {

    List<AlarmTypeRuleModel> selectListAndInfoRule(Set<String> ids);

    List<AlarmTypeRuleModel> selectListAndInfoRuleWhenDeviceEmpty();

    List<AlarmType> selectByDevice(String id);

}
