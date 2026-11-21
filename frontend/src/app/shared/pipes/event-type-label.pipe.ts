import { Pipe, PipeTransform } from '@angular/core';

import { EVENT_TYPES, EventType } from '../models/event-type.model';

/** Shows an event type as a readable label, for example "Breaking news". */
@Pipe({ name: 'eventTypeLabel' })
export class EventTypeLabelPipe implements PipeTransform {
  transform(value: EventType): string {
    return EVENT_TYPES.find((option) => option.value === value)?.label ?? value;
  }
}
