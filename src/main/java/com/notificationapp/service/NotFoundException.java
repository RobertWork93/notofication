package com.notificationapp.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.util.UUID;

/**
 * A requested resource does not exist. Spring turns it into a {@code 404} Problem Details response.
 */
public class NotFoundException extends ErrorResponseException {

    public NotFoundException(String resource, UUID id) {
        super(HttpStatus.NOT_FOUND,
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, resource + " " + id + " not found"), null);
    }

}
