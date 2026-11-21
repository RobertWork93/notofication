import { HttpErrorResponse } from '@angular/common/http';

import { httpErrorMessage } from './http-error-message';

describe('httpErrorMessage', () => {
  const errorWith = (status: number, error: unknown = null) => new HttpErrorResponse({ status, error });

  it.each([
    [0, 'The server cannot be reached. Is the backend running?'],
    [401, 'Wrong email or password.'],
    [403, 'You are not allowed to do this.'],
    [404, 'Not found. It may have been deleted.'],
  ])('should explain status %i', (status, message) => {
    expect(httpErrorMessage(errorWith(status))).toBe(message);
  });

  it('should use the Problem Details detail when there is one', () => {
    expect(httpErrorMessage(errorWith(400, { status: 400, detail: 'Invalid request content.' }))).toBe(
      'Invalid request content.',
    );
  });

  it('should fall back to the status code', () => {
    expect(httpErrorMessage(errorWith(500))).toBe('The request failed (status 500).');
  });

  it('should handle errors that are not HTTP errors', () => {
    expect(httpErrorMessage(new Error('boom'))).toBe('Something went wrong.');
  });
});
