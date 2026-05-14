import {
  ConflictException,
  Injectable,
  UnauthorizedException,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { InjectModel } from '@nestjs/mongoose';
import * as bcrypt from 'bcryptjs';
import { Model } from 'mongoose';

import { AccessLogWriter } from '../access-log/access-log-writer.service';
import { CompanyService } from '../company/company.service';
import { LoginDto, RegisterDto } from './dto';
import { User, UserDocument } from './user.schema';

@Injectable()
export class AuthService {
  constructor(
    @InjectModel(User.name) private readonly userModel: Model<UserDocument>,
    private readonly jwt: JwtService,
    private readonly companyService: CompanyService,
    private readonly accessLogWriter: AccessLogWriter,
  ) {}

  async register(dto: RegisterDto) {
    const existing = await this.userModel.findOne({ username: dto.username }).exec();
    if (existing) {
      throw new ConflictException('Username already taken');
    }
    await this.companyService.ensure(dto.companyCode, dto.companyName ?? dto.companyCode);
    const passwordHash = await bcrypt.hash(dto.password, 10);
    const user = await this.userModel.create({
      username: dto.username.toLowerCase(),
      passwordHash,
      companyCode: dto.companyCode,
      roles: dto.roles?.length ? dto.roles : ['user'],
    });
    return { id: user._id, username: user.username, companyCode: user.companyCode };
  }

  async login(dto: LoginDto, ctx: { ip?: string; path?: string; method?: string }) {
    const user = await this.userModel.findOne({ username: dto.username.toLowerCase() }).exec();
    if (!user || !user.active) {
      await this.accessLogWriter.writeUnauthorized({
        reason: 'INVALID_CREDENTIALS',
        sourceIp: ctx.ip,
        path: ctx.path,
        method: ctx.method,
        context: { username: dto.username },
      });
      throw new UnauthorizedException('Invalid credentials');
    }
    const ok = await bcrypt.compare(dto.password, user.passwordHash);
    if (!ok) {
      await this.accessLogWriter.writeUnauthorized({
        reason: 'INVALID_CREDENTIALS',
        userId: user._id?.toString(),
        companyId: user.companyCode,
        sourceIp: ctx.ip,
        path: ctx.path,
        method: ctx.method,
      });
      throw new UnauthorizedException('Invalid credentials');
    }

    const payload = {
      sub: user._id?.toString(),
      username: user.username,
      companyCode: user.companyCode,
      roles: user.roles,
    };
    const accessToken = await this.jwt.signAsync(payload);
    return {
      access_token: accessToken,
      token_type: 'Bearer',
      expires_in: parseInt(process.env.JWT_TTL_SECONDS ?? '3600', 10),
      user: { id: payload.sub, username: payload.username, companyCode: payload.companyCode, roles: payload.roles },
    };
  }

  async introspect(token: string) {
    try {
      const payload = await this.jwt.verifyAsync(token);
      return { active: true, ...payload };
    } catch {
      return { active: false };
    }
  }
}
