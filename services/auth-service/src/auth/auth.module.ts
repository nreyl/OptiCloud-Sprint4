import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { PassportModule } from '@nestjs/passport';
import { MongooseModule } from '@nestjs/mongoose';

import { AccessLogModule } from '../access-log/access-log.module';
import { CompanyModule } from '../company/company.module';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';
import { JwtAuthGuard } from './jwt-auth.guard';
import { JwtStrategy } from './jwt.strategy';
import { User, UserSchema } from './user.schema';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: User.name, schema: UserSchema }]),
    PassportModule,
    JwtModule.registerAsync({
      useFactory: () => ({
        secret: process.env.JWT_SECRET ?? 'change-me-in-prod',
        // issuer "opticloud" lets Kong's jwt plugin map the token to its
        // registered consumer credential and verify the signature at the gateway.
        signOptions: {
          expiresIn: `${process.env.JWT_TTL_SECONDS ?? '3600'}s`,
          issuer: process.env.JWT_ISSUER ?? 'opticloud',
        },
      }),
    }),
    AccessLogModule,
    CompanyModule,
  ],
  providers: [AuthService, JwtStrategy, JwtAuthGuard],
  controllers: [AuthController],
  exports: [AuthService],
})
export class AuthModule {}
