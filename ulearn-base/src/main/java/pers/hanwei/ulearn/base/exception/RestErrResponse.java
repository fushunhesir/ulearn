package pers.hanwei.ulearn.base.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class RestErrResponse implements Serializable {
    String errCode;

    String errMessage;

    public RestErrResponse(String errMessage) {
        this.errMessage = errMessage;
    }
}
