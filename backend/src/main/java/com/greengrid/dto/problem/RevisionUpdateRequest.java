package com.greengrid.dto.problem;

import com.greengrid.entity.RevisionStatus;

public record RevisionUpdateRequest(RevisionStatus revisionStatus, Boolean favorite) {
}
