package pers.hanwei.ulearn.base.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class TeachPlanDeleteException extends RuntimeException{

    private String errCode;

    private String errMessage;

    public TeachPlanDeleteException(String errCode, String errMessage) {
        this.errCode = errCode;
        this.errMessage = errMessage;
    }

    public void cast(String errCode, String errMessage) {
        throw new TeachPlanDeleteException(errCode, errMessage);
    }

}
