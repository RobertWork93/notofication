/** A page of results, as returned by the backend's list endpoints. */
export interface Page<T> {
  readonly content: readonly T[];
  readonly page: PageMetadata;
}

export interface PageMetadata {
  readonly size: number;
  readonly number: number;
  readonly totalElements: number;
  readonly totalPages: number;
}

export const DEFAULT_PAGE_SIZE = 20;
export const PAGE_SIZE_OPTIONS: readonly number[] = [10, 20, 50, 100];
