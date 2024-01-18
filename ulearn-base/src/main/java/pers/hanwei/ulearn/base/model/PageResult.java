package pers.hanwei.ulearn.base.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@AllArgsConstructor
public class PageResult<T> {
    private List<T> items;

    private Long counts;

    private Long page;

    private Long pageSize;
}
