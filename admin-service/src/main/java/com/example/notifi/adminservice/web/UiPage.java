package com.example.notifi.adminservice.web;

import com.example.notifi.adminservice.dto.PageResponse;
import java.util.List;

public class UiPage<T> {
  private final PageResponse<T> delegate;

  public UiPage(PageResponse<T> delegate) {
    this.delegate = delegate;
  }

  public List<T> getContent() {
    return delegate.getContent();
  }

  public int getNumber() {
    return delegate.getPage();
  }

  public int getSize() {
    return delegate.getSize();
  }

  public long getTotalElements() {
    return delegate.getTotalElements();
  }

  public int getTotalPages() {
    return delegate.getTotalPages();
  }

  public boolean hasPrevious() {
    return delegate.getPage() > 0;
  }

  public boolean hasNext() {
    return delegate.getPage() + 1 < Math.max(delegate.getTotalPages(), 1);
  }
}
