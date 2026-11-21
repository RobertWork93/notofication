/** The two hardcoded event types. Used by both alerts and events. */
export type EventType = 'breaking_news' | 'natural_disaster';

export interface EventTypeOption {
  readonly value: EventType;
  readonly label: string;
}

export const EVENT_TYPES: readonly EventTypeOption[] = [
  { value: 'breaking_news', label: 'Breaking news' },
  { value: 'natural_disaster', label: 'Natural disaster' },
];
