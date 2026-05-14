import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { MongooseModule } from '@nestjs/mongoose';

import { AuthModule } from './auth/auth.module';
import { CompanyModule } from './company/company.module';
import { AccessLogModule } from './access-log/access-log.module';
import { NotificationModule } from './notification/notification.module';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    MongooseModule.forRootAsync({
      useFactory: () => ({
        uri: process.env.MONGO_URI ?? 'mongodb://localhost:27017/opticloud_auth',
      }),
    }),
    NotificationModule,
    AccessLogModule,
    CompanyModule,
    AuthModule,
  ],
})
export class AppModule {}
