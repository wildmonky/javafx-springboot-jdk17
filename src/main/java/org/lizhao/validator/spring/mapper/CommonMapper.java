package org.lizhao.validator.spring.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CommonMapper<T> extends BaseMapper<T> {
    /**
     * 真正的批量插入
     * @param entityList
     * @return
     */
    int insertBatchSomeColumn(List<T> entityList);

    @Select(" SELECT SEQ.NEXTVAL NEXTVAL FROM DUAL ")
    long nextSeqId();
}
