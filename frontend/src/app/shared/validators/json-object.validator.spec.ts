import { FormControl } from '@angular/forms';

import { jsonObjectValidator } from './json-object.validator';

describe('jsonObjectValidator', () => {
  const validate = (value: string) => jsonObjectValidator(new FormControl(value, { nonNullable: true }));

  it('should accept an empty value, because the payload is optional', () => {
    expect(validate('')).toBeNull();
    expect(validate('   ')).toBeNull();
  });

  it('should accept a JSON object', () => {
    expect(validate('{"magnitude": 6.8, "tags": ["tsunami"]}')).toBeNull();
  });

  it.each(['[1, 2]', '42', '"text"', 'null', '{not json'])('should reject %s', (value) => {
    expect(validate(value)).toEqual({ jsonObject: true });
  });
});
