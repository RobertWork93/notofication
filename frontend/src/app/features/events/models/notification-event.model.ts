import { EventType } from '../../../shared/models/event-type.model';

/** Type-specific event details. Stored and returned exactly as sent. */
export type EventPayload = Readonly<Record<string, unknown>>;

/** An event that alerts can react to. Named NotificationEvent to avoid clashing with the DOM Event type. */
export interface NotificationEvent {
  readonly id: string;
  readonly type: EventType;
  readonly title: string;
  readonly source: string;
  readonly occurredAt: string;
  readonly payload: EventPayload | null;
  readonly receivedAt: string;
}

export interface CreateEventRequest {
  readonly type: EventType;
  readonly title: string;
  readonly source: string;
  readonly occurredAt: string;
  readonly payload?: EventPayload;
}
