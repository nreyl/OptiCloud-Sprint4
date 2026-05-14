import {
  CanActivate,
  ExecutionContext,
  ForbiddenException,
  Injectable,
} from '@nestjs/common';

import { AccessLogWriter } from '../access-log/access-log-writer.service';

/**
 * CompanyGuard enforces tenant isolation: a request can only touch a resource
 * owned by the company embedded in its JWT. The expected company can come
 * either from a route param (:companyCode), a header (x-company), or a
 * query string (?company=...).
 */
@Injectable()
export class CompanyGuard implements CanActivate {
  constructor(private readonly accessLogWriter: AccessLogWriter) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const req = context.switchToHttp().getRequest();
    const user = req.user;

    if (!user || !user.companyCode) {
      throw new ForbiddenException('Missing company context in token');
    }

    const expected =
      req.params?.companyCode ??
      req.headers?.['x-company'] ??
      req.query?.company ??
      user.companyCode;

    if (expected && expected !== user.companyCode) {
      await this.accessLogWriter.writeUnauthorized({
        reason: 'COMPANY_MISMATCH',
        userId: user.sub,
        companyId: user.companyCode,
        sourceIp: req.ip,
        path: req.originalUrl,
        method: req.method,
        context: { requestedCompany: expected },
      });
      throw new ForbiddenException('Company context mismatch');
    }

    return true;
  }
}
