import { HttpContextToken } from '@angular/common/http';

/**
 * Marks a request that handles its own credentials (login and sign-up).
 * The auth interceptor then neither adds the stored credentials nor redirects to login on 401.
 */
export const SKIP_AUTH = new HttpContextToken<boolean>(() => false);
