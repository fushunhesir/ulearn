package pers.hanwei.ulearn.base.exception;

import lombok.Data;

/**
 * 定义项目异常信息
 */
@Data
public class ULearnException extends RuntimeException{
        private String errMessage;

        public ULearnException(String errMessage) {
            this.errMessage = errMessage;
        }

        public void cast(String errMessage) {
            throw new ULearnException(errMessage);
        }

        public void cast(CommonError commonError) {
            throw new ULearnException(commonError.getErrMessage());
        }
}
