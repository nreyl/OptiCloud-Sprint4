import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';

import { AccessLogModule } from '../access-log/access-log.module';
import { CompanyGuard } from './company.guard';
import { Company, CompanySchema } from './company.schema';
import { CompanyService } from './company.service';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Company.name, schema: CompanySchema }]),
    AccessLogModule,
  ],
  providers: [CompanyService, CompanyGuard],
  exports: [CompanyService, CompanyGuard],
})
export class CompanyModule {}
