import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type CompanyDocument = Company & Document;

@Schema({ collection: 'companies', timestamps: true })
export class Company {
  @Prop({ required: true, unique: true })
  code: string;

  @Prop({ required: true })
  name: string;

  @Prop({ default: true })
  active: boolean;
}

export const CompanySchema = SchemaFactory.createForClass(Company);
