import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type AccessLogDocument = AccessLog & Document;

@Schema({ collection: 'unauthorized_access_logs', timestamps: { createdAt: 'occurredAt', updatedAt: false } })
export class AccessLog {
  @Prop({ required: true })
  reason: string;

  @Prop()
  userId?: string;

  @Prop()
  companyId?: string;

  @Prop()
  sourceIp?: string;

  @Prop()
  path?: string;

  @Prop()
  method?: string;

  @Prop({ type: Object })
  context?: Record<string, unknown>;
}

export const AccessLogSchema = SchemaFactory.createForClass(AccessLog);
