package pers.hanwei.ulearn.base.model;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class PageParams {
    private Long pageNo = 1L;

    private Long pageSize = 30L;


    // why constructor?
    public PageParams(){}

    public PageParams(Long pageNo, Long pageSize) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }
}
