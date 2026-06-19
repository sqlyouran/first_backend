package com.mooc.app.dto.response;

import java.util.List;

public class MessageListResponse extends BaseResponse {
    private final List<MessageItemResponse> items;
    private final long total;
    private final int page;
    private final int size;

    public MessageListResponse(String requestId, List<MessageItemResponse> items,
                                long total, int page, int size) {
        super(requestId);
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
    }
    public List<MessageItemResponse> getItems() { return items; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}
