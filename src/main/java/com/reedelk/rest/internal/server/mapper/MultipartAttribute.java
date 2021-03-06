package com.reedelk.rest.internal.server.mapper;

import com.reedelk.rest.internal.commons.HttpHeader;

public interface MultipartAttribute {

    String FILE_NAME = "filename";

    String CONTENT_TYPE = HttpHeader.CONTENT_TYPE;

    String TRANSFER_ENCODING = HttpHeader.TRANSFER_ENCODING;

}
