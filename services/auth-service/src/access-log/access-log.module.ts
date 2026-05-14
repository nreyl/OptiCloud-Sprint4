import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';

import { NotificationModule } from '../notification/notification.module';
import { AccessLogWriter } from './access-log-writer.service';
import { AccessLog, AccessLogSchema } from './access-log.schema';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: AccessLog.name, schema: AccessLogSchema }]),
    NotificationModule,
  ],
  providers: [AccessLogWriter],
  exports: [AccessLogWriter],
})
export class AccessLogModule {}
