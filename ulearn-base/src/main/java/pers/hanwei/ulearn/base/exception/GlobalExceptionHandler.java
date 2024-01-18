package pers.hanwei.ulearn.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局异常捕获器
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    // 自定义异常处理方法: 向前端返回异常信息
    @ResponseBody
    @ExceptionHandler(ULearnException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrResponse customException(ULearnException ue) {
        log.error("[系统异常]{}", ue.getErrMessage(), ue);
        return new RestErrResponse(ue.getErrMessage());
    }

    @ResponseBody
    @ExceptionHandler(TeachPlanDeleteException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrResponse teachPlanDeleteException(TeachPlanDeleteException te) {
        return new RestErrResponse(te.getErrCode(), te.getErrMessage());
    }

    // 非自定义异常处理
    @ResponseBody
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrResponse systemException(Exception e) {
        log.error("【系统异常】{}", e.getMessage(), e);
        return new RestErrResponse(CommonError.UNKOWN_ERROR.getErrMessage());
    }


    /**
     * DTO参数合法性校验
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public RestErrResponse argumentNotValidException(MethodArgumentNotValidException e) {
        // 返回错误信息列表
        List<String> errMessages = new ArrayList<>();

        // 获取错误信息列表
        BindingResult bindingResult = e.getBindingResult();
        bindingResult.getFieldErrors().stream().forEach(item->errMessages.add(item.getDefaultMessage()));
        return new RestErrResponse(errMessages.toString());
    }
}
