package org.lizhao.validator.spring.model;

import lombok.Getter;
import lombok.Setter;
import org.lizhao.validator.spring.entity.AlarmType;

@Getter
@Setter
public class AlarmTypeRuleModel extends AlarmType {

    private String ruleId;

    private String ruleName;

    private String ruleDesc;

    private String rulePatternId;

    private Boolean ruleIncludeDeviceName;

    // 全文匹配
    private Boolean ruleFullTextMatch;

}
