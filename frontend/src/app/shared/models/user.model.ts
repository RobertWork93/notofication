export type Role = 'ADMIN' | 'USER';

export interface User {
  readonly id: string;
  readonly email: string;
  readonly name: string;
  readonly role: Role;
  readonly createdAt: string;
  readonly updatedAt: string;
}

/** Body of sign-up and of a user update. The password is always required. */
export interface UserRequest {
  readonly email: string;
  readonly name: string;
  readonly password: string;
}
