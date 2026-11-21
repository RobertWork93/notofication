import { EventType } from '../../../shared/models/event-type.model';

export type Channel = 'email' | 'slack';

export interface ChannelOption {
  readonly value: Channel;
  readonly label: string;
}

export const CHANNELS: readonly ChannelOption[] = [
  { value: 'email', label: 'Email' },
  { value: 'slack', label: 'Slack' },
];

export interface Alert {
  readonly id: string;
  readonly userId: string;
  readonly name: string;
  readonly eventType: EventType;
  readonly channel: Channel;
  readonly enabled: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}

/** Fields an alert's owner can set. Used for both create and update. */
export interface AlertFields {
  readonly name: string;
  readonly eventType: EventType;
  readonly channel: Channel;
  readonly enabled: boolean;
}

export interface CreateAlertRequest extends AlertFields {
  readonly userId: string;
}

export type UpdateAlertRequest = AlertFields;
