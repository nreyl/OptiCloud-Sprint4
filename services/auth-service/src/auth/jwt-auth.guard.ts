import { ExecutionContext, Injectable, UnauthorizedException } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';

import { AccessLogWriter } from '../access-log/access-log-writer.service';

@Injectable()
export class JwtAuthGuard extends AuthGuard('jwt') {
  constructor(private readonly accessLogWriter: AccessLogWriter) {
    super();
  }

  handleRequest(err: any, user: any, info: any, context: ExecutionContext) {
    if (err || !user) {
      const req = context.switchToHttp().getRequest();
      this.accessLogWriter
        .writeUnauthorized({
          reason: 'INVALID_TOKEN',
          sourceIp: req?.ip,
          path: req?.originalUrl,
          method: req?.method,
          context: { info: info?.message ?? String(info) },
        })
        .catch(() => undefined);
      throw err || new UnauthorizedException('Invalid or missing token');
    }
    return user;
  }
}
