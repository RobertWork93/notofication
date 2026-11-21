import { HttpErrorResponse } from '@angular/common/http';

/** The part of a Spring Problem Details body the frontend shows. */
interface ProblemDetail {
  readonly detail?: string;
}

/** Turns a failed request into a message for the user. */
export function httpErrorMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'Something went wrong.';
  }
  switch (error.status) {
    case 0:
      return 'The server cannot be reached. Is the backend running?';
    case 401:
      return 'Wrong email or password.';
    case 403:
      return 'You are not allowed to do this.';
    case 404:
      return 'Not found. It may have been deleted.';
    default:
      return isProblemDetail(error.error) && error.error.detail
        ? error.error.detail
        : `The request failed (status ${error.status}).`;
  }
}

function isProblemDetail(body: unknown): body is ProblemDetail {
  return typeof body === 'object' && body !== null && 'detail' in body;
}
