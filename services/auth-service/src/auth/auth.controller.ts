import {
  Body,
  Controller,
  Get,
  Post,
  Req,
  UseGuards,
} from '@nestjs/common';
import type { Request } from 'express';

import { CompanyGuard } from '../company/company.guard';
import { AuthService } from './auth.service';
import { IntrospectDto, LoginDto, RegisterDto } from './dto';
import { JwtAuthGuard } from './jwt-auth.guard';

@Controller()
export class AuthController {
  constructor(private readonly auth: AuthService) {}

  @Get('health')
  health() {
    return { status: 'ok', service: 'auth-service' };
  }

  @Post('register')
  register(@Body() dto: RegisterDto) {
    return this.auth.register(dto);
  }

  @Post('login')
  login(@Body() dto: LoginDto, @Req() req: Request) {
    return this.auth.login(dto, {
      ip: req.ip,
      path: req.originalUrl,
      method: req.method,
    });
  }

  @Post('introspect')
  introspect(@Body() dto: IntrospectDto) {
    return this.auth.introspect(dto.token);
  }

  @UseGuards(JwtAuthGuard)
  @Get('me')
  me(@Req() req: Request) {
    return req.user;
  }

  @UseGuards(JwtAuthGuard, CompanyGuard)
  @Get('company/:companyCode/check')
  companyCheck(@Req() req: Request) {
    return { ok: true, user: req.user };
  }
}
