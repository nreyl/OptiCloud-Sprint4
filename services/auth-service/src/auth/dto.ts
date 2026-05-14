import { IsArray, IsOptional, IsString, MinLength } from 'class-validator';

export class LoginDto {
  @IsString() @MinLength(3)
  username: string;

  @IsString() @MinLength(6)
  password: string;
}

export class RegisterDto {
  @IsString() @MinLength(3)
  username: string;

  @IsString() @MinLength(6)
  password: string;

  @IsString()
  companyCode: string;

  @IsString() @IsOptional()
  companyName?: string;

  @IsArray() @IsOptional()
  roles?: string[];
}

export class IntrospectDto {
  @IsString()
  token: string;
}
