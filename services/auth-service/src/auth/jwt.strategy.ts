import { Injectable, UnauthorizedException } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';

import { AccessLogWriter } from '../access-log/access-log-writer.service';

export interface JwtPayload {
  sub: string;
  username: string;
  companyCode: string;
  roles: string[];
}

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy, 'jwt') {
  constructor(private readonly accessLogWriter: AccessLogWriter) {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      secretOrKey: process.env.JWT_SECRET ?? 'change-me-in-prod',
      passReqToCallback: true,
    });
  }

  async validate(req: any, payload: JwtPayload) {
    if (!payload?.sub || !payload?.companyCode) {
      await this.accessLogWriter.writeUnauthorized({
        reason: 'INVALID_TOKEN',
        sourceIp: req?.ip,
        path: req?.originalUrl,
        method: req?.method,
        context: { payload },
      });
      throw new UnauthorizedException('Invalid token payload');
    }
    return payload;
  }
}
