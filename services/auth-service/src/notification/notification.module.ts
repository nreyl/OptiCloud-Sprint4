import { Module } from '@nestjs/common';
import { NotificationClient } from './notification.client';

@Module({
  providers: [NotificationClient],
  exports: [NotificationClient],
})
export class NotificationModule {}
