import { Injectable, Logger } from '@nestjs/common';
import axios from 'axios';

export interface SecurityIncidentPayload {
  incident_type:
    | 'UNAUTHORIZED_ACCESS'
    | 'INVALID_TOKEN'
    | 'COMPANY_MISMATCH'
    | 'EXCESSIVE_FAILED_LOGIN';
  company_id?: string;
  user_id?: string;
  source_ip?: string;
  path?: string;
  detail?: string;
}

@Injectable()
export class NotificationClient {
  private readonly logger = new Logger(NotificationClient.name);
  private readonly baseUrl =
    process.env.NOTIFICATION_URL ?? 'http://localhost:8080/notifications';

  async reportIncident(payload: SecurityIncidentPayload): Promise<void> {
    try {
      await axios.post(`${this.baseUrl}/incidents`, payload, { timeout: 3000 });
    } catch (err) {
      this.logger.warn(
        `Could not deliver security incident to notification-service: ${(err as Error).message}`,
      );
    }
  }
}
