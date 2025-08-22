package org.lizhao.validator.mapper;

import org.lizhao.validator.entity.AlarmType;
import org.lizhao.validator.model.AlarmTypeRuleModel;

import java.util.List;
import java.util.Set;

public interface AlarmTypeMapper extends CommonMapper<AlarmType> {

    List<AlarmTypeRuleModel> selectListAndInfoRule(Set<String> ids);

    List<AlarmTypeRuleModel> selectListAndInfoRuleWhenDeviceEmpty();

    List<AlarmType> selectByDevice(String id);

}
