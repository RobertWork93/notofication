import { Pipe, PipeTransform } from '@angular/core';

import { CHANNELS, Channel } from '../models/alert.model';

/** Shows a channel as a readable label, for example "Slack". */
@Pipe({ name: 'channelLabel' })
export class ChannelLabelPipe implements PipeTransform {
  transform(value: Channel): string {
    return CHANNELS.find((option) => option.value === value)?.label ?? value;
  }
}
