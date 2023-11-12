package com.davolab.web.rest.template.dto;

import java.io.Serializable;

public class PaginationDto implements Serializable {

    private static final long serialVersionUID = 6560239736776971781L;

    private int page;
    private int size;
    private long total;
    private int totalPages;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
