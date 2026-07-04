package com.mooc.app.dto.response;

public class AiPostAssistResponse extends BaseResponse {

    private final String result;

    public AiPostAssistResponse(String requestId, String result) {
        super(requestId);
        this.result = result;
    }

    public String getResult() { return result; }
}
