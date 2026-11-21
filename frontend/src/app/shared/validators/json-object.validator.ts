import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Accepts an empty value or text that parses to a JSON object (not an array or a plain value). */
export const jsonObjectValidator: ValidatorFn = (control: AbstractControl<string>): ValidationErrors | null => {
  const text = control.value.trim();
  if (!text) {
    return null;
  }
  try {
    const parsed: unknown = JSON.parse(text);
    return typeof parsed === 'object' && parsed !== null && !Array.isArray(parsed) ? null : { jsonObject: true };
  } catch {
    return { jsonObject: true };
  }
};
