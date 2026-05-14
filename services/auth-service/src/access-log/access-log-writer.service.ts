import { Injectable, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';

import { NotificationClient } from '../notification/notification.client';
import { AccessLog, AccessLogDocument } from './access-log.schema';

export interface AccessLogEntry {
  reason: string;
  userId?: string;
  companyId?: string;
  sourceIp?: string;
  path?: string;
  method?: string;
  context?: Record<string, unknown>;
}

@Injectable()
export class AccessLogWriter {
  private readonly logger = new Logger(AccessLogWriter.name);

  constructor(
    @InjectModel(AccessLog.name) private readonly accessLogModel: Model<AccessLogDocument>,
    private readonly notifications: NotificationClient,
  ) {}

  async writeUnauthorized(entry: AccessLogEntry): Promise<void> {
    try {
      await this.accessLogModel.create(entry);
    } catch (err) {
      this.logger.error(`Failed to persist access log: ${(err as Error).message}`);
    }

    await this.notifications.reportIncident({
      incident_type: this.mapReasonToIncident(entry.reason),
      company_id: entry.companyId,
      user_id: entry.userId,
      source_ip: entry.sourceIp,
      path: entry.path,
      detail: entry.reason,
    });
  }

  private mapReasonToIncident(reason: string) {
    switch (reason) {
      case 'INVALID_CREDENTIALS':
        return 'EXCESSIVE_FAILED_LOGIN' as const;
      case 'INVALID_TOKEN':
        return 'INVALID_TOKEN' as const;
      case 'COMPANY_MISMATCH':
        return 'COMPANY_MISMATCH' as const;
      default:
        return 'UNAUTHORIZED_ACCESS' as const;
    }
  }
}
