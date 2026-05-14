import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type UserDocument = User & Document;

@Schema({ collection: 'users', timestamps: true })
export class User {
  @Prop({ required: true, unique: true, lowercase: true })
  username: string;

  @Prop({ required: true })
  passwordHash: string;

  @Prop({ required: true, index: true })
  companyCode: string;

  @Prop({ type: [String], default: ['user'] })
  roles: string[];

  @Prop({ default: true })
  active: boolean;
}

export const UserSchema = SchemaFactory.createForClass(User);
